package application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

public class MessagingSupervisor implements Runnable {
	
	private Thread thread;
	private MessagingSettings config;
	private boolean isPublishMode = false;
	private boolean isEventSubscribeMode = false;
	private boolean isIoTPublishMode = false;
	private boolean isIoTEventSubscribeMode = false;
	private ConnectionFactory factory = new ConnectionFactory();
	private ExecutorService subscriberThreadPool;
	private ExecutorService iotSubscriberThreadPool;
	private Connection supervisorConnection;
	private Connection publishConnection;
	private Connection subscribeConnection;
	private Connection iotSupervisorConnection;
	private Connection iotPublishConnection;
	private Connection iotSubscribeConnection;
	private Channel supervisorChannel;
	private Channel iotSupervisorChannel;
	private ConcurrentMessageStore publishStore;
	private List<MessagePublisher> publishers = new ArrayList<>();
	private List<MessageSubscriber> eventSubscribers = new ArrayList<>();
	private ConcurrentMessageStore iotPublishStore;
	private List<MessagePublisher> iotPublishers = new ArrayList<>();
	private List<MessageSubscriber> iotEventSubscribers = new ArrayList<>();
	private MessagingListener listener;
	
	public MessagingSupervisor(MessagingSettings settings, ConcurrentMessageStore publishStore, 
			ConcurrentMessageStore iotPublishStore, MessagingListener listener) throws IOException {
		processConfig(settings);
		this.publishStore = publishStore;
		this.iotPublishStore = iotPublishStore;
		openConnectionsAndChannels();
		createExchangeAndQueue();
		this.listener = listener;
	}
	
	public void start() {
		if (thread != null) return;
		thread = new Thread(this);
		thread.start();
	}
	
	public synchronized void interrupt() {
		closeWorkers();
		if (listener != null) listener.statusMessageNotification("Supervisor terminated.");
		thread.interrupt();
	}
	
	@Override
	public void run() {
		if (listener != null) listener.statusMessageNotification("Supervisor started.");
		while (!Thread.currentThread().isInterrupted()) supervise();
		closeResources();
	}
	
	private void processConfig(MessagingSettings settings) {
		config = settings;
		isPublishMode = (config.getMaxPublishers() > 0);
		isEventSubscribeMode = (config.getMaxEventSubscribers() > 0);
		isIoTPublishMode = (config.getMaxIoTPublishers() > 0);
		isIoTEventSubscribeMode = (config.getMaxIoTEventSubscribers() > 0);
	}
	
	private void openConnectionsAndChannels() throws IOException {
		if (isPublishMode || isEventSubscribeMode) supervisorConnection = setupConnection("Supervisor", false, false);
		if (supervisorConnection != null) supervisorChannel = supervisorConnection.createChannel();
		if (isIoTPublishMode || isIoTEventSubscribeMode) iotSupervisorConnection = setupConnection("IoT Supervisor", false, true);
		if (iotSupervisorConnection != null) iotSupervisorChannel = iotSupervisorConnection.createChannel();
		if (isPublishMode) publishConnection = setupConnection("Publisher", false, false);
		if (isEventSubscribeMode) subscribeConnection = setupConnection("Subscriber", true, false);
		if (isIoTPublishMode) iotPublishConnection = setupConnection("IoT Publisher", false, true);
		if (isIoTEventSubscribeMode) iotSubscribeConnection = setupConnection("IoT Subscriber", true, true);
	}
	
	private Connection setupConnection(String connName, boolean isSubscriber, boolean isIoT) {
		Connection conn = null;
		factory.setHost(isIoT ? config.getIoTHost() : config.getHost());
		factory.setPort(isIoT ? config.getIoTPort() : config.getPort());
		factory.setVirtualHost(isIoT ? config.getIoTVHost() : config.getVHost());
		factory.setUsername(isIoT ? config.getIoTUsername() : config.getUsername());
		factory.setPassword(isIoT ? config.getIoTPassword() : config.getPassword());
		try {
			if (isSubscriber && isIoT) {
				iotSubscriberThreadPool = Executors.newFixedThreadPool(config.getIoTSubscriberThreadPoolSize());
				conn = factory.newConnection(iotSubscriberThreadPool, connName);
			}
			else if (isSubscriber) {
				subscriberThreadPool = Executors.newFixedThreadPool(config.getSubscriberThreadPoolSize());
				conn = factory.newConnection(subscriberThreadPool, connName);
			}
			else {
				conn = factory.newConnection(connName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		return conn;
	}
	
	private void closeWorkers() {
		interruptPublishers(iotPublishers);
		interruptPublishers(publishers);
		terminateSubscribers(iotEventSubscribers);
		terminateSubscribers(eventSubscribers);
	}
	
	private void closeResources() {
		try {
			closeConnection(iotSubscribeConnection);
			closeConnection(iotPublishConnection);
			closeConnection(subscribeConnection);
			closeConnection(publishConnection);
			if (isIoTEventSubscribeMode) stopSubThreadPool(iotSubscriberThreadPool);
			if (isEventSubscribeMode) stopSubThreadPool(subscriberThreadPool);
			closeChannel(iotSupervisorChannel);
			closeConnection(iotSupervisorConnection);
			closeChannel(supervisorChannel);
			closeConnection(supervisorConnection);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void closeConnection(Connection connection) throws IOException {
		if (connection == null) return;
		if (!connection.isOpen()) return;
		connection.close();
	}
	
	private void closeChannel(Channel channel) throws IOException, TimeoutException {
		if (channel == null) return;
		if (!channel.isOpen()) return;
		channel.close();
	}
	
	private void interruptPublishers(List<MessagePublisher> publishers) {
		publishers.forEach(p -> p.interrupt());
	}
	
	private void terminateSubscribers(List<MessageSubscriber> subscribers) {
		subscribers.forEach(c -> c.terminateGracefully());
	}
	
	private void stopSubThreadPool(ExecutorService subThreadPool) {
		if (subThreadPool == null) return;
		subThreadPool.shutdown();
		try {
            while(!subThreadPool.awaitTermination(10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
        	Thread.currentThread().interrupt();
        }
	}
	
	private void createExchangeAndQueue() {
		try {
			if (supervisorChannel != null) {
				supervisorChannel.exchangeDeclare(config.getExchangeName(), "topic", true);
				supervisorChannel.queueDeclare(config.getEventQueueName(), true, false, false, null);
			}
			if (iotSupervisorChannel != null) {
				iotSupervisorChannel.exchangeDeclare(config.getIoTExchangeName(), "topic", true);
				iotSupervisorChannel.queueDeclare(config.getIoTEventQueueName(), true, false, false, null);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void supervise() {
		doSupervise();
		try {
			Thread.sleep(config.getSupervisorInterval());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	private void doSupervise() {// must survive any exception
		try {
			supervisePubSub();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void supervisePubSub() throws IOException {
		supervisePublishers();
		superviseEventSubscribers();
		superviseIoTPublishers();
		superviseIoTEventSubscribers();
	}
	
	private void supervisePublishers() {
		if (!isPublishMode) return;
		if (!publishConnection.isOpen()) return;
		if (!supervisorChannel.isOpen()) return;
		int msgCount = publishStore.size();
		int pubCount = publishers.size();
		if (pubCount < config.getMinPublishers()) addPublishers(config.getMinPublishers() - pubCount, false);
		else if (msgCount > 1 && pubCount < config.getMaxPublishers()) addPublishers(1, false); // 1 per pass
		else if (msgCount == 0 && pubCount > config.getMinPublishers()) removePublishers(1, false); // 1 per pass
		if (listener != null) listener.publishingStatusNotification(msgCount, publishers.size());
	}
	
	private void superviseEventSubscribers() throws IOException {
		if (!isEventSubscribeMode) return;
		if (!subscribeConnection.isOpen()) return;
		DeclareOk status = supervisorChannel.queueDeclarePassive(config.getEventQueueName());
		int msgCount = status.getMessageCount();
		int subCount = status.getConsumerCount();
		if (subCount < config.getMinEventSubscribers()) addEventSubscribers(config.getMinEventSubscribers() - subCount, false);
		else if (msgCount > 1 && subCount < config.getMaxEventSubscribers()) addEventSubscribers(1, false); // 1 per pass
		else if (msgCount == 0 && subCount > config.getMinEventSubscribers()) removeEventSubscribers(1, false); // 1 per pass
		if (listener != null) listener.subscribingStatusNotification(msgCount, subCount);
	}
	
	private void superviseIoTPublishers() {
		if (!isIoTPublishMode) return;
		if (!iotPublishConnection.isOpen()) return;
		if (!iotSupervisorChannel.isOpen()) return;
		int msgCount = iotPublishStore.size();
		int pubCount = iotPublishers.size();
		if (pubCount < config.getMinIoTPublishers()) addPublishers(config.getMinIoTPublishers() - pubCount, true);
		else if (msgCount > 1 && pubCount < config.getMaxIoTPublishers()) addPublishers(1, true); // 1 per pass
		else if (msgCount == 0 && pubCount > config.getMinIoTPublishers()) removePublishers(1, true); // 1 per pass
		if (listener != null) listener.publishingIoTStatusNotification(msgCount, iotPublishers.size());
	}
	
	private void superviseIoTEventSubscribers() throws IOException {
		if (!isIoTEventSubscribeMode) return;
		if (!iotSubscribeConnection.isOpen()) return;
		DeclareOk status = iotSupervisorChannel.queueDeclarePassive(config.getIoTEventQueueName());
		int msgCount = status.getMessageCount();
		int subCount = status.getConsumerCount();
		if (subCount < config.getMinIoTEventSubscribers()) addEventSubscribers(config.getMinIoTEventSubscribers() - subCount, true);
		else if (msgCount > 1 && subCount < config.getMaxIoTEventSubscribers()) addEventSubscribers(1, true); // 1 per pass
		else if (msgCount == 0 && subCount > config.getMinIoTEventSubscribers()) removeEventSubscribers(1, true); // 1 per pass
		if (listener != null) listener.subscribingIoTStatusNotification(msgCount, subCount);
	}
	
	private void addPublishers(int count, boolean isIoT) {
		IntStream.rangeClosed(1, count).forEach(i -> addPublisher(isIoT));
	}
	
	private void removePublishers(int count, boolean isIoT) {
		IntStream.rangeClosed(1, count).forEach(i -> removePublisher(isIoT));
	}
	
	private void addEventSubscribers(int count, boolean isIoT) {
		IntStream.rangeClosed(1, count).forEach(i -> addEventSubscriber(isIoT));
	}
	
	private void removeEventSubscribers(int count, boolean isIoT) {
		IntStream.rangeClosed(1, count).forEach(i -> removeEventSubscriber(isIoT));
	}
	
	private void addPublisher(boolean isIoT) {
		boolean mode = isIoT ? isIoTPublishMode : isPublishMode;
		if (!mode) return;
		Connection connection = isIoT ? iotPublishConnection : publishConnection;
		String exchangeName = isIoT ? config.getIoTExchangeName() : config.getExchangeName();
		ConcurrentMessageStore pubStore = isIoT ? iotPublishStore : publishStore;
		List<MessagePublisher> pubList = isIoT ? iotPublishers : publishers;
		String pubId = (isIoT ? "IoT_" : "") + "Publisher_" + String.valueOf(pubList.size() + 1);
		BasicProperties msgType = isIoT ? MessageProperties.MINIMAL_BASIC : MessageProperties.PERSISTENT_TEXT_PLAIN;
		try {
			MessagePublisher publisher = new MessagePublisher(connection, exchangeName, pubStore, pubId, msgType, listener);
			pubList.add(publisher);
			publisher.start();
			if (listener != null) listener.statusMessageNotification((isIoT ? "IoT " : "") + 
					"Publisher count: " + String.valueOf(pubList.size()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void removePublisher(boolean isIoT) {
		List<MessagePublisher> pubList = isIoT ? iotPublishers : publishers;
		if (pubList.size() == 0) return;
    	MessagePublisher p = pubList.remove(pubList.size() - 1);
    	p.interrupt();
    	if (listener != null) listener.statusMessageNotification((isIoT ? "IoT " : "") + 
    			"Publisher count: " + String.valueOf(pubList.size()));
	}
	
	private void addEventSubscriber(boolean isIoT) {
		boolean mode = isIoT ? isIoTEventSubscribeMode : isEventSubscribeMode;
		if (!mode) return;
		Connection connection = isIoT ? iotSubscribeConnection : subscribeConnection;
		String exchangeName = isIoT ? config.getIoTExchangeName() : config.getExchangeName();
		String eventQueueName = isIoT ? config.getIoTEventQueueName() : config.getEventQueueName();
		List<String> topicPatterns = isIoT ? config.getIoTEventTopicPatterns() : config.getEventTopicPatterns();
		List<MessageSubscriber> subList = isIoT ? iotEventSubscribers : eventSubscribers;
		String subId = (isIoT ? "IoT_" : "") + "Subscriber " + String.valueOf(subList.size() + 1);
		try {
			subList.add(new MessageSubscriber(connection, exchangeName, eventQueueName, topicPatterns, subId, listener));
			if (listener != null) listener.statusMessageNotification((isIoT ? "IoT " : "") + 
					"Subscriber count: " + String.valueOf(eventSubscribers.size()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void removeEventSubscriber(boolean isIoT) {
		List<MessageSubscriber> subList = isIoT ? iotEventSubscribers : eventSubscribers;
		if (subList.size() == 0) return;
    	MessageSubscriber c = subList.remove(subList.size() - 1);
    	c.terminateGracefully();
    	if (listener != null) listener.statusMessageNotification((isIoT ? "IoT " : "") + 
    			"Subscriber count: " + String.valueOf(eventSubscribers.size()));
	}
	
}
