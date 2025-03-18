//llmrequest
package org.example.gangazido_be.llm.model;

public class LlmRequest {
	private String message;
	private double latitude;
	private double longitude;

	public LlmRequest() {
	}

	public LlmRequest(String message, double latitude, double longitude) {
		this.message = message;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public String getMessage() {
		return message;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}
}
