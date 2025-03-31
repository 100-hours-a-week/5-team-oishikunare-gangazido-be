package org.example.gangazido_be.llm.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class LlmResponse implements Serializable {
	private String message;
	private Data data;

	public LlmResponse(String message, String response) {
		this.message = message;
		this.data = new Data(response);
	}

	public LlmResponse(String message) {
		this.message = message;
		this.data = null;
	}

	public String getMessage() {
		return message;
	}

	public Data getData() {
		return data;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setData(Data data) {
		this.data = data;
	}

	//  여기가 핵심!
	@JsonIgnore
	public String getResponse() {
		if (data != null) {
			return data.getResponse();
		}
		return null;
	}

	public static class Data implements Serializable {
		private String response;

		public Data(String response) {
			this.response = response;
		}

		public String getResponse() {
			return response;
		}

		public void setResponse(String response) {
			this.response = response;
		}
	}
}
