//llmresponse

package org.example.gangazido_be.llm.model;

public class LlmResponse {
	private String message;
	private String response;

	public LlmResponse(String message, String response) {
		this.message = message;
		this.response = response;
	}

	public String getMessage() {
		return message;
	}

	public String getResponse() {
		return response;
	}
}

