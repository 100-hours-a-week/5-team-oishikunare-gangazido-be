package org.example.gangazido_be.user.exception;

public class UserAuthenticationException extends UserException {
	public static final String INVALID_CREDENTIALS = "invalid_email_or_password";
	public static final String INVALID_EMAIL = "invalid_email";
	public static final String INVALID_PASSWORD = "invalid_password";
	public static final String UNAUTHORIZED = "required_authorization";
	public static final String MISSING_USER = "user_not_found";
	public static final String INVALID_CURRENT_PASSWORD = "invalid_current_password";

	public UserAuthenticationException(String errorCode, String message) {
		super(errorCode, message);
	}

	public static UserAuthenticationException invalidCredentials() {
		return new UserAuthenticationException(INVALID_CREDENTIALS, "invalid_email_or_password");
	}

	public static UserAuthenticationException invalidEmail() {
		return new UserAuthenticationException(INVALID_EMAIL, "invalid_email");
	}

	public static UserAuthenticationException invalidPassword() {
		return new UserAuthenticationException(INVALID_PASSWORD, "invalid_password");
	}

	public static UserAuthenticationException unauthorized() {
		return new UserAuthenticationException(UNAUTHORIZED, "required_authentication");
	}

	public static UserAuthenticationException missingUser() {
		return new UserAuthenticationException(MISSING_USER, "user_not_found");
	}

	public static UserAuthenticationException invalidCurrentPassword() {
		return new UserAuthenticationException(INVALID_CURRENT_PASSWORD, "invalid_current_password");
	}
}
