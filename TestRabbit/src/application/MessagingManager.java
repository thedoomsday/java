package application;

import shared.MessageStructure;

public class MessagingManager {
	
	private ConcurrentMessageStore publishStore;
	private ConcurrentMessageStore iotPublishStore;
	private MessagingSupervisor supervisor;
	
	public MessagingManager(MessagingListener listener) throws Exception {
		publishStore = new ConcurrentMessageStore(listener);
		iotPublishStore = new ConcurrentMessageStore(listener);
		supervisor = new MessagingSupervisor(new MessagingSettings(), publishStore, iotPublishStore, listener);
		supervisor.start();
	}
	
	public MessagingManager() throws Exception {
		this(null);
	}
	
	public void close() {
		supervisor.interrupt();
	}
	
	public void publishMessage(MessageStructure message) throws InterruptedException {
		publishStore.push(message);
	}
	
	public void publishIotMessage(MessageStructure message) throws InterruptedException {
		iotPublishStore.push(message);
	}
	
}
