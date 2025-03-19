package org.example.gangazido_be.pet.common;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
@JsonPropertyOrder({"message", "data"})
public class ApiResponse {
	private String message;
	private Object data;
}
