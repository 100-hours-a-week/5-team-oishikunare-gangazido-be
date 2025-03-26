package org.example.gangazido_be.gpt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAiConfig {

	@Value("${openai.api-key}")
	private String apiKey;

	public String getApiKey() {
		return apiKey;
	}
}
