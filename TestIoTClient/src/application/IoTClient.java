package application;

import java.io.UnsupportedEncodingException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONObject;

import javafx.application.Platform;

import shared.MessageStructure;
import shared.UIReporter;

public class IoTClient implements MqttCallbackExtended {
	
	private String statusTopic = "ievent/clientstatus";
	private String testSubscribeTopic = "iot/test";
	private String clientId;
	private MqttClient client;
	
	private UIReporter reporter;
	
	public IoTClient(String clientId, UIReporter reporter) throws Exception {
		this.reporter = reporter;
		
		this.clientId = clientId;
		setupConn();
	}
	
	public void close() {
		if (!client.isConnected()) return;
		try {
			//iotClient.disconnect();  //crashes message broker web socket server internally *only* when lwt is set
			client.disconnectForcibly(30000, 10000, false); //so we call disconnectForcibly with default timeout values and sendDisconnectPacket set to *false*
			client.close();
		} catch (MqttException e) {
			e.printStackTrace();
		} 
	}
	
	public void publish(MessageStructure message) {
		try {
    		client.publish(signatureToTopic(message.getSignature()), message.getPayload(), 0, false);
    	} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void connectionLost(Throwable cause) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				String msg = clientId + ": Connection is lost. Reconnecting...";
				reporter.reportConnectionStatus(msg);
				reporter.addOutput(msg);
			}
		});
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String msg = new String(message.getPayload(), "UTF-8");
		reporter.addOutput(clientId + ": IoT message arrived with topic " + topic + ": " + msg);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		//no need to implement as we use QoS 0		
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		reporter.setDefaultStageTitle();
		if (!reconnect) reporter.addOutput(clientId + ": waiting for messages.");
		try {
			new Thread(new Runnable () {
				@Override
				public void run() {
					try {
						client.publish(statusTopic, getStatusPayload(false), 0, false);
					} catch (UnsupportedEncodingException | MqttException e) {
						e.printStackTrace();
					}
				}
			}).start();
			client.subscribe(testSubscribeTopic, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setupConn() throws MqttException, UnsupportedEncodingException {
		//String host = "ws://localhost:15675/ws"; //ws direct
		//String host = "tcp://localhost:1883";    //native direct
		String host = "ws://localhost:8081/ws";  //ws IIS
		//String host = "ws://localhost:8083/ws";  //ws HAProxy in wsl
		//String host = "tcp://localhost:8084";    //native HAProxy in wsl
		//String host = "ws://localhost:8085/ws"; //ws nginx
		//String host = "ws://localhost:8086/ws"; //ws gobetween
		//String host = "tcp://localhost:8087"; //native gobetween
		        
        String tmpDir = System.getProperty("java.io.tmpdir");
    	MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
        MqttConnectOptions conOpt = new MqttConnectOptions();
        conOpt.setUserName("1");
        conOpt.setPassword("1".toCharArray());
        conOpt.setKeepAliveInterval(30);
        conOpt.setAutomaticReconnect(true);
        conOpt.setWill(statusTopic, getStatusPayload(true), 0, false);
        client = new MqttClient(host, clientId, dataStore);
        client.setCallback(this);
        client.connect(conOpt);
	}
	
	private byte[] getStatusPayload(boolean isDisconnect) throws UnsupportedEncodingException {
		JSONObject data = new JSONObject();
    	data.put("status", isDisconnect ? 0 : 1);
		return new MessageStructure(topicToSignature(statusTopic), data).getPayload();
	}
	
	private String topicToSignature(String topic) {
		return topic.replace('/', '.');
	}
	
	private String signatureToTopic(String signature) {
		return signature.replace('.', '/');
	}
	
}
