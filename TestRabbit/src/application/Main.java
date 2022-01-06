package application;
	
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicLong;

import shared.MessageStructure;
import shared.UIReporter;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Insets;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class Main extends Application {
	
	private TextField txtPublishRate = new TextField("1");
    private Button btnPublish = new Button("Start publishing");
    private Label lblPublishQueue = new Label("-");
    private Label lblPublishers = new Label("-");
    private Label lblSubscribers = new Label("-");
	private TextField txtIoTPublishRate = new TextField("1");
    private Button btnIoTPublish = new Button("Start publishing");
    private Label lblIoTPublishQueue = new Label("-");
    private Label lblIoTPublishers = new Label("-");
    private Label lblIoTSubscribers = new Label("-");
	private TextField txtIoTS2CPublishRate = new TextField("1");
    private Button btnIoTS2CPublish = new Button("Start publishing");
	
	private boolean isPublishing = false;
	private boolean isIoTPublishing = false;
	private boolean isIoTS2CPublishing = false;
	private final AtomicLong messageCount = new AtomicLong(0);
	private final AtomicLong iotMessageCount = new AtomicLong(0);
	private final AtomicLong iotS2CMessageCount = new AtomicLong(0);
	public static UIReporter reporter;
	public static MessagingManager messagingManager;
	private Thread publisher;
	private Thread iotPublisher;
	private Thread iotS2CPublisher;
		
	@Override
	public void start(Stage primaryStage) {
		try {
	        GridPane root = new GridPane();
	        root.setPadding(new Insets(10, 10, 10, 10));
	        addControls(root);
	        setControlHooks();
	        reporter = new UIReporter(lblPublishQueue, lblPublishers, lblSubscribers, 
	        		lblIoTPublishQueue, lblIoTPublishers, lblIoTSubscribers, null, null, null);
	        messagingManager = new MessagingManager(reporter);
			Scene scene = new Scene(root, 690, 500);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setResizable(false);
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void stop() throws Exception {
		Thread.sleep(1000);//to consume the lwt and show it in console
		if (publisher != null) publisher.interrupt();
		publisher = null;
		if (iotPublisher != null) iotPublisher.interrupt();
		iotPublisher = null;
		if (iotS2CPublisher != null) iotS2CPublisher.interrupt();
		iotS2CPublisher = null;
		messagingManager.close();
		messagingManager = null;
		super.stop();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	private void addControls(GridPane root) {
		GridPane S2S = new GridPane();
		S2S.setPadding(new Insets(5, 0, 0, 0));
		S2S.setHgap(4);
		S2S.setVgap(4);
		Label lblS2S = new Label("Server to server internal infrastructure (AMQP)");
		lblS2S.setFont(Font.font(null, FontWeight.BOLD, 12));
		lblS2S.setUnderline(true);
		S2S.add(lblS2S, 0, 0, 3, 1);
		S2S.add(new Label("Publish rate per second"), 0, 1);
		S2S.add(txtPublishRate, 0, 2);
		S2S.add(btnPublish, 1, 2);
		S2S.add(new Label("Publishing queue size"), 0, 3);
		S2S.add(lblPublishQueue, 0, 4);
		S2S.add(new Label("Publishers"), 1, 3);
		S2S.add(lblPublishers, 1, 4);
		S2S.add(new Label("Subscribers"), 2, 3);
		S2S.add(lblSubscribers, 2, 4);
		root.add(S2S, 0, 0);
		Pane hPlaceHolder1 = new Pane();
		hPlaceHolder1.setMinWidth(40);
		root.add(hPlaceHolder1, 1, 0);
		GridPane iotS2S = new GridPane();
		iotS2S.setPadding(new Insets(5, 0, 0, 0));
		iotS2S.setHgap(4);
		iotS2S.setVgap(4);
		Label lblIoTS2S = new Label("Server to server IoT infrastructure (AMQP)");
		lblIoTS2S.setFont(Font.font(null, FontWeight.BOLD, 12));
		lblIoTS2S.setUnderline(true);
		iotS2S.add(lblIoTS2S, 0, 0, 3, 1);
		iotS2S.add(new Label("Publish rate per second"), 0, 1);
		iotS2S.add(txtIoTPublishRate, 0, 2);
		iotS2S.add(btnIoTPublish, 1, 2);
		iotS2S.add(new Label("Publishing queue size"), 0, 3);
		iotS2S.add(lblIoTPublishQueue, 0, 4);
		iotS2S.add(new Label("Publishers"), 1, 3);
		iotS2S.add(lblIoTPublishers, 1, 4);
		iotS2S.add(new Label("Subscribers"), 2, 3);
		iotS2S.add(lblIoTSubscribers, 2, 4);
		root.add(iotS2S, 2, 0);
		Pane vPlaceHolder1 = new Pane();
		vPlaceHolder1.setMinHeight(10);
		root.add(vPlaceHolder1, 0, 1);
		GridPane iotS2C = new GridPane();
		iotS2C.setPadding(new Insets(5, 0, 0, 0));
		iotS2C.setHgap(4);
		iotS2C.setVgap(4);
		Label lblIoTS2C = new Label("IoT Server to Client (AMQP to MQTT)");
		lblIoTS2C.setFont(Font.font(null, FontWeight.BOLD, 12));
		lblIoTS2C.setUnderline(true);
		iotS2C.add(lblIoTS2C, 0, 0, 3, 1);
		iotS2C.add(new Label("Publish rate per second"), 0, 1);
		iotS2C.add(txtIoTS2CPublishRate, 0, 2);
		iotS2C.add(btnIoTS2CPublish, 1, 2);
		root.add(iotS2C, 0, 2);
	}
	
	private void setControlHooks() {
		EventHandler<KeyEvent> numOnly = new EventHandler<KeyEvent>() {
        	@Override
        	public void handle(KeyEvent keyEvent) {
        		if (!"0123456789".contains(keyEvent.getCharacter())) {
        			keyEvent.consume();
        		}
        	}
        };
        EventHandler<ActionEvent> publishHandler = new EventHandler<ActionEvent>() {
    	    @Override
    	    public void handle(ActionEvent event) {
    	    	if (!isPublishing) {
    	    		isPublishing = true;
    	    		btnPublish.setText("Stop publishing");
    	    		txtPublishRate.setEditable(false);
    	    		startPublishing();
    	    	}
    	    	else {
    	    		isPublishing = false;
    	    		btnPublish.setText("Start publishing");
    	    		txtPublishRate.setEditable(true);
    	    		stopPublishing();
    	    	}
    	    }
    	};
    	EventHandler<ActionEvent> iotPublishHandler = new EventHandler<ActionEvent>() {
    	    @Override
    	    public void handle(ActionEvent event) {
    	    	if (!isIoTPublishing) {
    	    		isIoTPublishing = true;
    	    		btnIoTPublish.setText("Stop publishing");
    	    		txtIoTPublishRate.setEditable(false);
    	    		startIoTPublishing();
    	    	}
    	    	else {
    	    		isIoTPublishing = false;
    	    		btnIoTPublish.setText("Start publishing");
    	    		txtIoTPublishRate.setEditable(true);
    	    		stopIoTPublishing();
    	    	}
    	    }
    	};
    	EventHandler<ActionEvent> iotS2CPublishHandler = new EventHandler<ActionEvent>() {
    	    @Override
    	    public void handle(ActionEvent event) {
    	    	if (!isIoTS2CPublishing) {
    	    		isIoTS2CPublishing = true;
    	    		btnIoTS2CPublish.setText("Stop publishing");
    	    		txtIoTS2CPublishRate.setEditable(false);
    	    		startIoTS2CPublishing();
    	    	}
    	    	else {
    	    		isIoTS2CPublishing = false;
    	    		btnIoTS2CPublish.setText("Start publishing");
    	    		txtIoTS2CPublishRate.setEditable(true);
    	    		stopIoTS2CPublishing();
    	    	}
    	    }
    	};
		btnPublish.setOnAction(publishHandler);
        txtPublishRate.addEventFilter(KeyEvent.KEY_TYPED, numOnly);
        btnIoTPublish.setOnAction(iotPublishHandler);
        txtIoTPublishRate.addEventFilter(KeyEvent.KEY_TYPED, numOnly);
        btnIoTS2CPublish.setOnAction(iotS2CPublishHandler);
        txtIoTS2CPublishRate.addEventFilter(KeyEvent.KEY_TYPED, numOnly);
	}
	
	private void startPublishing() {
		long rate = 0;
		try {
			rate = Integer.valueOf(txtPublishRate.getText());
		} catch (NumberFormatException e) {
			stopPublishing();
			return;
		}
		if (rate < 1 || rate > 1000000) {
			stopPublishing();
			return;
		}
		final long waitMicros = (1000000 / rate);
		publisher = new Thread(new Runnable() {
			@Override
			public void run () {
				while (!Thread.currentThread().isInterrupted()) {
					publish();
					microWait(waitMicros);
				}
			}
		});
		publisher.start();
	}
	
	private void startIoTPublishing() {
		long rate = 0;
		try {
			rate = Integer.valueOf(txtIoTPublishRate.getText());
		} catch (NumberFormatException e) {
			stopIoTPublishing();
			return;
		}
		if (rate < 1 || rate > 1000000) {
			stopIoTPublishing();
			return;
		}
		final long waitMicros = (1000000 / rate);
		iotPublisher = new Thread(new Runnable() {
			@Override
			public void run () {
				while (!Thread.currentThread().isInterrupted()) {
					iotPublish();
					microWait(waitMicros);
				}
			}
		});
		iotPublisher.start();
	}
	
	private void startIoTS2CPublishing() {
		long rate = 0;
		try {
			rate = Integer.valueOf(txtIoTS2CPublishRate.getText());
		} catch (NumberFormatException e) {
			stopIoTS2CPublishing();
			return;
		}
		if (rate < 1 || rate > 1000000) {
			stopIoTS2CPublishing();
			return;
		}
		final long waitMicros = (1000000 / rate);
		iotS2CPublisher = new Thread(new Runnable() {
			@Override
			public void run () {
				while (!Thread.currentThread().isInterrupted()) {
					iotS2CPublish();
					microWait(waitMicros);
				}
			}
		});
		iotS2CPublisher.start();
	}
	
	private static void microWait(long micros){
        long waitUntil = System.nanoTime() + (micros * 1000);
        while(waitUntil > System.nanoTime()){
            ;
        }
    }
	
	private void stopPublishing() {
		messageCount.set(0);
		if (publisher == null) return;
		publisher.interrupt();
		publisher = null;
	}
	
	private void stopIoTPublishing() {
		iotMessageCount.set(0);
		if (iotPublisher == null) return;
		iotPublisher.interrupt();
		iotPublisher = null;
	}
	
	private void stopIoTS2CPublishing() {
		iotS2CMessageCount.set(0);
		if (iotS2CPublisher == null) return;
		iotS2CPublisher.interrupt();
		iotS2CPublisher = null;
	}
	
	private void publish() {
		JSONObject data = new JSONObject();
		long msgNo = messageCount.incrementAndGet();
    	data.put("attr", "The messageααα " + String.valueOf(msgNo));
    	try {
			messagingManager.publishMessage(new MessageStructure("event.test", data));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void iotPublish() {
		JSONObject data = new JSONObject();
		long msgNo = iotMessageCount.incrementAndGet();
    	data.put("attr", "The messageααα " + String.valueOf(msgNo));
    	try {
			messagingManager.publishIotMessage(new MessageStructure("ievent.test", data));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void iotS2CPublish() {
		JSONObject data = new JSONObject();
		long msgNo = iotS2CMessageCount.incrementAndGet();
    	data.put("attr", "The messageααα " + String.valueOf(msgNo));
    	try {
			messagingManager.publishIotMessage(new MessageStructure("iot.test", data));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
};
