package shared;

import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.json.JSONObject;

public class MessageStructure {
	
	private static final String messageEncoding = "UTF-8";
	private String signature;
	private JSONObject data;
	private byte[] payload;
	
	public MessageStructure(String signature, JSONObject data) throws UnsupportedEncodingException, JSONException {
		this.signature = signature;
		this.data = data;
		pack();
	}
	
	public MessageStructure(byte[] payload) throws JSONException, UnsupportedEncodingException {
		this.payload = payload;
		unpack();
	}
	
	public String getSignature() {
		return signature;
	}
	
	public JSONObject getData() {
		return data;
	}
	
	public byte[] getPayload() {
		return payload;
	}

	public String getString() throws UnsupportedEncodingException {
		return new String(payload, messageEncoding);
	}
	
	private void pack() throws UnsupportedEncodingException, JSONException {
		payload = new JSONObject().put("signature", signature).put("data", data)
			.toString().getBytes(messageEncoding);
	}
	
	private void unpack() throws JSONException, UnsupportedEncodingException {
		JSONObject jsonPayload = new JSONObject(new String(payload, messageEncoding));
		signature = jsonPayload.getString("signature");
		data = jsonPayload.getJSONObject("data");
	}
	
}
