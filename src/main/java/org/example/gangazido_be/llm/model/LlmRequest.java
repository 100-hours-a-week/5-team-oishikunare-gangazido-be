package org.example.gangazido_be.llm.model;

public class LlmRequest {
	private double latitude;
	private double longitude;
	private String message;

	public LlmRequest() {}  // 기본 생성자

	public LlmRequest(double latitude, double longitude, String message) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.message = message;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
