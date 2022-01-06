package application;

import shared.MessageStructure;
import shared.UIReporter;

public class MessagingManager {
	
	private ConcurrentMessageStore publishStore = new ConcurrentMessageStore();
	private ConcurrentMessageStore iotPublishStore = new ConcurrentMessageStore();
	private MessagingSupervisor supervisor;
	
	public MessagingManager(UIReporter reporter) throws Exception {
		supervisor = new MessagingSupervisor(new MessagingSettings(), publishStore, iotPublishStore, reporter);
		supervisor.start();
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
