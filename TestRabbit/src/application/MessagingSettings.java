package application;

import java.util.Arrays;
import java.util.List;

public class MessagingSettings {
	
	private String host = "127.0.0.1";
	private int port = 5672;
	private String vHost = "/";
	private String username = "guest";
	private String password = "guest";
	private String exchangeName = "test_exchange";
	private String eventQueueName = "test_event_queue";
	private List<String> eventTopicPatterns = Arrays.asList("event.#");
	private int subscriberThreadPoolSize = 2;
	private int minPublishers = 1;
	private int maxPublishers = 2;
	private int minEventSubscribers = 1;
	private int maxEventSubscribers = 2;
	private String iotHost = "127.0.0.1";
	private int iotPort = 5672;
	private String iotVHost = "iot";
	private String iotUsername = "guest";
	private String iotPassword = "guest";
	private String iotExchangeName = "amq.topic";
	private String iotEventQueueName = "test_iot_event_queue";
	private List<String> iotEventTopicPatterns = Arrays.asList("ievent.#");
	private int iotSubscriberThreadPoolSize = 1;
	private int minIoTPublishers = 1;
	private int maxIoTPublishers = 2;
	private int minIoTEventSubscribers = 1;
	private int maxIoTEventSubscribers = 2;
	private int supervisorInterval = 1000; //milliseconds
	
	public MessagingSettings() {
		//Load from configuration
	}
	
	public String getHost() {
		return host;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getVHost() {
		return vHost;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getExchangeName() {
		return exchangeName;
	}
	
	public String getEventQueueName() {
		return eventQueueName;
	}
	
	public List<String> getEventTopicPatterns() {
		return eventTopicPatterns;
	}
	
	public int getSubscriberThreadPoolSize() {
		return subscriberThreadPoolSize;
	}
	
	public int getMinPublishers() {
		return minPublishers;
	}
	
	public void setMinPublishers(int value) {
		minPublishers = value;
	}
	
	public int getMaxPublishers() {
		return maxPublishers;
	}
	
	public void setMaxPublishers(int value) {
		maxPublishers = value;
	}
	
	public int getMinEventSubscribers() {
		return minEventSubscribers;
	}
	
	public void setMinEventSubscribers(int value) {
		minEventSubscribers = value;
	}
	
	public int getMaxEventSubscribers() {
		return maxEventSubscribers;
	}
	
	public void setMaxEventSubscribers(int value) {
		maxEventSubscribers = value;
	}
	
	public String getIoTHost() {
		return iotHost;
	}
	
	public int getIoTPort() {
		return iotPort;
	}
	
	public String getIoTVHost() {
		return iotVHost;
	}
	
	public String getIoTUsername() {
		return iotUsername;
	}
	
	public String getIoTPassword() {
		return iotPassword;
	}
	
	public String getIoTExchangeName() {
		return iotExchangeName;
	}
	
	public String getIoTEventQueueName() {
		return iotEventQueueName;
	}
	
	public List<String> getIoTEventTopicPatterns() {
		return iotEventTopicPatterns;
	}
	
	public int getIoTSubscriberThreadPoolSize() {
		return iotSubscriberThreadPoolSize;
	}
	
	public int getMinIoTPublishers() {
		return minIoTPublishers;
	}
	
	public void setMinIoTPublishers(int value) {
		minIoTPublishers = value;
	}
	
	public int getMaxIoTPublishers() {
		return maxIoTPublishers;
	}
	
	public void setMaxIoTPublishers(int value) {
		maxIoTPublishers = value;
	}
	
	public int getMinIoTEventSubscribers() {
		return minIoTEventSubscribers;
	}
	
	public void setMinIoTEventSubscribers(int value) {
		minIoTEventSubscribers = value;
	}
	
	public int getMaxIoTEventSubscribers() {
		return maxIoTEventSubscribers;
	}
	
	public void setMaxIoTEventSubscribers(int value) {
		maxIoTEventSubscribers = value;
	}
	
	public int getSupervisorInterval() {
		return supervisorInterval;
	}
	
	public void setSupervisorInterval(int value) {
		supervisorInterval = value;
	}
	
}
