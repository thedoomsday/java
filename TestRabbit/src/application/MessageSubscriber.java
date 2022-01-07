package application;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import shared.MessageStructure;

public class MessageSubscriber {
	
	private final AtomicBoolean isWorking = new AtomicBoolean(false);
	private final AtomicBoolean isMarkedForTermination = new AtomicBoolean(false);
	private final AtomicBoolean isTerminated = new AtomicBoolean(false);
	private Channel channel;
	private String exchangeName;
	private String queueName;
	private List<String> topicPatterns;
	private String subscriberId;
	private MessagingListener listener;
	
	public MessageSubscriber(Connection conn, String exchangeName, String queueName, 
			List<String> topicPatterns, String subscriberId, MessagingListener listener) throws IOException {
		this.channel = conn.createChannel();
		this.exchangeName = exchangeName;
		this.queueName = queueName;
		this.topicPatterns = topicPatterns;
		this.subscriberId = subscriberId;
		this.listener = listener;
		startListening();
	}
	
	public void terminateGracefully() {
		if (isWorking.get())
			isMarkedForTermination.set(true);
		else 
			terminateForcefully();
	}
	
	public boolean hasTerminated() {
		return isTerminated.get();
	}
	
	public synchronized void terminateForcefully() {
		try {
			if (!channel.isOpen()) return;
			channel.basicCancel(subscriberId);
			channel.close();
			isTerminated.set(true);
			if (listener != null) listener.statusMessageNotification(subscriberId  + " in Thread ID " + 
					String.valueOf(Thread.currentThread().getId()) + " terminated.");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void startListening() throws IOException {
		channel.exchangeDeclare(exchangeName, "topic", true);
		channel.queueDeclare(queueName, true, false, false, null);
		topicPatterns.forEach(this::bindPattern);
		channel.basicQos(1);
        channel.basicConsume(queueName, false, subscriberId, getConsumer());
        if (listener != null) listener.statusMessageNotification(subscriberId + " in Thread ID " + 
        		String.valueOf(Thread.currentThread().getId()) + " (main thread): waiting for messages.");
	}
	
	private DefaultConsumer getConsumer() {
		return new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
            	try {
	            	if (envelope.isRedeliver()) {
	            		getChannel().basicAck(envelope.getDeliveryTag(), false);
	            		return;
	            	}
	            	isWorking.set(true);
	            	MessageStructure message = new MessageStructure(body);
	            	if (listener != null) listener.statusMessageNotification(consumerTag + 
	            			" in ThreadID " + String.valueOf(Thread.currentThread().getId()) + 
	                		": received " + message.getString() + "\r\n" + "with routing key " + envelope.getRoutingKey());
                	unpackRun(message);
                	if (getChannel().isOpen()) getChannel().basicAck(envelope.getDeliveryTag(), false);
                	isWorking.set(false);
                	if (isMarkedForTermination.get()) terminateGracefully();
                } catch (Exception e) {
        			e.printStackTrace();
        		}
            }
        };
	}
	
	private void bindPattern(String topicPattern) {
		try {
			channel.queueBind(queueName, exchangeName, topicPattern);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void unpackRun(MessageStructure message) throws UnsupportedEncodingException {
		if (listener != null) listener.statusMessageNotification(subscriberId + 
				" in ThreadID " + String.valueOf(Thread.currentThread().getId()) + 
				": working with message content: " + message.getData().toString() + 
				" from topic " + message.getSignature());
		//Thread.sleep(1000); //simulating work
		if (listener != null) listener.statusMessageNotification(subscriberId + " in ThreadID " + 
				String.valueOf(Thread.currentThread().getId()) + 
				": work done with " + message.getString());
    }
	
}
