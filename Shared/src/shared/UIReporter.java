package shared;

import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class UIReporter {
	
	private Label lblPublishQueue;
	private Label lblPublishers;
	private Label lblSubscribers;
	private Label lblIoTPublishQueue;
	private Label lblIoTPublishers;
	private Label lblIoTSubscribers;
	private Stage reportStage;
	private String reportStageTitle;
	private TextArea txtOutput;
	private AtomicInteger outputIncrement = new AtomicInteger(0);
	
	public UIReporter(Label pubQueueLabel, Label pubsLabel, Label subsLabel, Label iotPubQueueLabel, 
			Label iotPubsLabel, Label iotSubsLabel, Stage primaryStage, String defaultStageTitle, TextArea output) {
		lblPublishQueue = pubQueueLabel;
		lblPublishers = pubsLabel;
		lblSubscribers = subsLabel;
		lblIoTPublishQueue = iotPubQueueLabel;
		lblIoTPublishers = iotPubsLabel;
		lblIoTSubscribers = iotSubsLabel;
		reportStage = primaryStage;
		reportStageTitle = defaultStageTitle;
		txtOutput = output;
	}
	
	public void reportPublishingStatus(int pubQueueSize, int publishers, boolean isIoT) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (isIoT) {
					lblIoTPublishQueue.setText(String.valueOf(pubQueueSize));
					lblIoTPublishers.setText(String.valueOf(publishers));
				}
				else {
					lblPublishQueue.setText(String.valueOf(pubQueueSize));
					lblPublishers.setText(String.valueOf(publishers));
				}
			}
		});
	}
	
	public void reportSubscribingStatus(int subscribers, boolean isIoT) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (isIoT) {
					lblIoTSubscribers.setText(String.valueOf(subscribers));
				}
				else {
					lblSubscribers.setText(String.valueOf(subscribers));
				}
			}
		});
	}
	
	public void reportConnectionStatus(String status) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				reportStage.setTitle(status);	
			}
		});
	}
	
	public void setDefaultStageTitle() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				reportStage.setTitle(reportStageTitle);	
			}
		});
	}
	
	public void addOutput(String text) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				int i = outputIncrement.incrementAndGet();
				if (i > 500) {
					txtOutput.setText("");
					outputIncrement.set(0);
				}
				txtOutput.appendText(text + "\r\n");
			}
		});
	}
	
}
