package org.example.gangazido_be.pet.exception;

public class UserNotFoundException extends RuntimeException {
	public UserNotFoundException() {
		super(PetExceptionType.NOT_FOUND_USER.getMessage());
	}
}
