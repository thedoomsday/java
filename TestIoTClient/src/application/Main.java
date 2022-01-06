package application;
	
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Insets;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import shared.MessageStructure;
import shared.UIReporter;

public class Main extends Application {
	
    private Label lblIoTC2SPublishRate = new Label("Publish rate per second");
   	private TextField txtIoTC2SPublishRate = new TextField("1");
    private Button btnIoTC2SPublish = new Button("Start publishing");
    private TextField txtIoTClients = new TextField("-");
    private Button btnAddIotClient = new Button("Add IoT client");
    private TextArea txtOutput = new TextArea();
	
	private boolean isPublishing = false;
	private final AtomicLong messageCount = new AtomicLong(0);
	public static UIReporter reporter;
	private Thread publisher;
	private List<IoTClient> iotClients = new ArrayList<>();
		
	@Override
	public void start(Stage primaryStage) {
		try {
	        GridPane root = new GridPane();
	        root.setPadding(new Insets(10, 10, 10, 10));
	        addControls(root);
	        setControlHooks();
	        reporter = new UIReporter(null, null, null,	null, null, null, primaryStage, "Messaging test", txtOutput);
	        addIotClient();
			Scene scene = new Scene(root, 550, 300);
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
		iotClients.forEach(c -> c.close());
		if (publisher != null) publisher.interrupt();
		publisher = null;
		super.stop();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	private void addIotClient() throws Exception {
        String clientId = "MQTT-Test-Client " + String.valueOf(iotClients.size() + 1);
        iotClients.add(new IoTClient(clientId, reporter));
        txtIoTClients.setText(String.valueOf(iotClients.size()));
	}
	
	private void addControls(GridPane root) {
		GridPane iotC = new GridPane();
		iotC.setPadding(new Insets(5, 0, 0, 0));
		iotC.setHgap(4);
		iotC.setVgap(4);
		Label lblIoTClientsTitle = new Label("IoT Clients");
		lblIoTClientsTitle.setFont(Font.font(null, FontWeight.BOLD, 12));
		lblIoTClientsTitle.setUnderline(true);
		iotC.add(lblIoTClientsTitle, 0, 0, 3, 1);
		txtIoTClients.setEditable(false);
		iotC.add(txtIoTClients, 0, 1, 2, 1);
		iotC.add(btnAddIotClient, 2, 1);
		root.add(iotC, 0, 0);
		Pane hPlaceHolder1 = new Pane();
		hPlaceHolder1.setMinWidth(40);
		root.add(hPlaceHolder1, 1, 0);
		GridPane iotC2S = new GridPane();
		iotC2S.setPadding(new Insets(5, 0, 0, 0));
		iotC2S.setHgap(4);
		iotC2S.setVgap(4);
		lblIoTC2SPublishRate.setFont(Font.font(null, FontWeight.BOLD, 12));
		lblIoTC2SPublishRate.setUnderline(true);
		iotC2S.add(lblIoTC2SPublishRate, 0, 1);
		iotC2S.add(txtIoTC2SPublishRate, 0, 2);
		iotC2S.add(btnIoTC2SPublish, 1, 2);
		root.add(iotC2S, 2, 0);
		Pane vPlaceHolder2 = new Pane();
		vPlaceHolder2.setMinHeight(10);
		root.add(vPlaceHolder2, 0, 1);
		root.add(new Label("IoT Client output"), 0, 2, 3, 1);
		txtOutput.setMinHeight(204);
		root.add(txtOutput, 0, 3, 3, 1);
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
    	EventHandler<ActionEvent> iotC2SPublishHandler = new EventHandler<ActionEvent>() {
    	    @Override
    	    public void handle(ActionEvent event) {
    	    	if (!isPublishing) {
    	    		isPublishing = true;
    	    		btnIoTC2SPublish.setText("Stop publishing");
    	    		txtIoTC2SPublishRate.setEditable(false);
    	    		startPublishing();
    	    	}
    	    	else {
    	    		isPublishing = false;
    	    		btnIoTC2SPublish.setText("Start publishing");
    	    		txtIoTC2SPublishRate.setEditable(true);
    	    		stopPublishing();
    	    	}
    	    }
    	};
    	EventHandler<ActionEvent> addIoTClientHandler = new EventHandler<ActionEvent>() {
    	    @Override
    	    public void handle(ActionEvent event) {
    	    	try {
					addIotClient();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    	    }
    	};
        btnIoTC2SPublish.setOnAction(iotC2SPublishHandler);
        txtIoTC2SPublishRate.addEventFilter(KeyEvent.KEY_TYPED, numOnly);
        btnAddIotClient.setOnAction(addIoTClientHandler);
	}
	
	private void startPublishing() {
		long rate = 0;
		try {
			rate = Integer.valueOf(txtIoTC2SPublishRate.getText());
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
					if (messageCount.get() == 0) return;
					microWait(waitMicros);
				}
			}
		});
		publisher.start();
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
	
	private synchronized void publish() {
		JSONObject data = new JSONObject();
		long msgNo = messageCount.incrementAndGet();
    	data.put("attr", "The message" + String.valueOf(msgNo));
    	MessageStructure message;
		try {
			message = new MessageStructure("ievent.test", data);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
    	iotClients.forEach(c -> c.publish(message));
	}
	
};
