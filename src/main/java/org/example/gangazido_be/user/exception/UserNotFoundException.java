package org.example.gangazido_be.user.exception;

public class UserNotFoundException extends UserException {
	public UserNotFoundException(String message) {
		super("user_not_found", message);
	}
}
