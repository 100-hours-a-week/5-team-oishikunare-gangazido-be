package org.example.gangazido_be.user.exception;

public class UserDuplicateException extends UserException {
	public static final String DUPLICATE_EMAIL = "duplicate_email";
	public static final String DUPLICATE_NICKNAME = "duplicate_nickname";

	public UserDuplicateException(String errorCode, String message) {
		super(errorCode, message);
	}

	public static UserDuplicateException duplicateEmail() {
		return new UserDuplicateException(DUPLICATE_EMAIL, "duplicate_email");
	}

	public static UserDuplicateException duplicateNickname() {
		return new UserDuplicateException(DUPLICATE_NICKNAME, "duplicate_nickname");
	}
}
