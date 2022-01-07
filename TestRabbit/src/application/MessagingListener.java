package application;

public interface MessagingListener {
	public void publishingStatusNotification(int messageCount, int publisherCount);
	public void subscribingStatusNotification(int messageCount, int subscriberCount);
	public void publishingIoTStatusNotification(int messageCount, int publisherCount);
	public void subscribingIoTStatusNotification(int messageCount, int subscriberCount);
	public void statusMessageNotification(String message);
}
