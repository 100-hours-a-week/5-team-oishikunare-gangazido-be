package org.example.gangazido_be.pet.exception;

import lombok.Getter;

import org.springframework.http.HttpStatus;

@Getter
public class PetException extends RuntimeException {
	private final HttpStatus status;
	private final String message;

	public PetException(HttpStatus status, String message) {
		super(message);
		this.status = status;
		this.message = message;
	}
}
