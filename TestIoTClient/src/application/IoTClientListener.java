package application;

import shared.MessageStructure;

public interface IoTClientListener {
	public void connectionFirstEstablished(String clientId);
	public void connectionLost(String clientId);
	public void connectionReestablished(String clientId);
	public void messagePublished(String clientId, String topic, MessageStructure message);
	public void messageReceived(String clientId, String topic, MessageStructure message);
	public void connectionClosed(String clientId);
}
