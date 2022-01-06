package application;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import shared.MessageStructure;

public class MessagePublisher implements Runnable {
	
	private Thread thread;
	private Channel channel;
	private String publisherId;
	private String exchangeName;
	private BasicProperties messageType;
	private ConcurrentMessageStore messageStore;
	
	public MessagePublisher(Connection conn, String exchangeName, ConcurrentMessageStore messageStore, 
			String publisherId, BasicProperties messageType) throws IOException {
		this.exchangeName = exchangeName;
		this.messageStore = messageStore;
		this.publisherId = publisherId;
		this.messageType = messageType;
		this.channel = conn.createChannel();
		this.channel.exchangeDeclare(exchangeName, "topic", true);
	}
	
	public void start() {
		if (thread != null) return;
		thread = new Thread(this);
		thread.start();
	}
	
	public synchronized void interrupt() {
		closeChannel();
		thread.interrupt();
	}
	
	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) publish();
		closeChannel();
		System.out.println(publisherId + ", Thread ID " + String.valueOf(Thread.currentThread().getId()) + " terminated.");
	}
	
	private void closeChannel() {
		try {
			if (channel.isOpen()) channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}
	
	private void publish() {
		MessageStructure message = popMessage();
		if (message == null) return;
		try {
			if (channel.isOpen()) {
				channel.basicPublish(exchangeName, message.getSignature(), messageType, message.getPayload());
				System.out.println(publisherId + ", Thread ID " + String.valueOf(Thread.currentThread().getId()) + " published message.");
			}
			else {
				System.out.println(publisherId + ", Thread ID " + String.valueOf(Thread.currentThread().getId()) + " requeueing message because connection is lost.");
				reQueueMessage(message);
				Thread.sleep(500); //wait for connection to recover
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
		} 
	}
	
	private MessageStructure popMessage() {
		MessageStructure message = null;
		try {
			message = messageStore.pop();
		} catch (InterruptedException e) {
			System.out.println(publisherId + ", Thread ID " + String.valueOf(Thread.currentThread().getId()) + " was interrupted.");
			Thread.currentThread().interrupt();
		}
		return message;
	}
	
	private void reQueueMessage(MessageStructure message) {
		try {
			messageStore.push(message);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
