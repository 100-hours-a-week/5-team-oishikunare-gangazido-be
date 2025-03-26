package org.example.gangazido_be.user.exception;

public class UserValidationException extends UserException {
	public static final String REQUIRED_USERID = "required_user_id";
	public static final String REQUIRED_EMAIL = "required_email";
	public static final String INVALID_EMAIL_FORMAT = "invalid_email_format";
	public static final String REQUIRED_PASSWORD = "required_password";
	public static final String REQUIRED_PASSWORD_CONFIRM = "required_password_confirm";
	public static final String INVALID_PASSWORD_FORMAT = "invalid_password_format";
	public static final String INVALID_PASSWORD_LENGTH = "invalid_password_length";
	public static final String REQUIRED_NICKNAME = "required_nickname";
	public static final String INVALID_NICKNAME_LENGTH = "invalid_nickname_length";
	public static final String PASSWORDS_DO_NOT_MATCH = "passwords_do_not_match";

	public UserValidationException(String errorCode, String message) {
		super(errorCode, message);
	}

	public static UserValidationException requiredUserId() {
		return new UserValidationException(REQUIRED_USERID, "required_user_id");
	}

	public static UserValidationException requiredEmail() {
		return new UserValidationException(REQUIRED_EMAIL, "required_email");
	}

	public static UserValidationException invalidEmailFormat() {
		return new UserValidationException(INVALID_EMAIL_FORMAT, "invalid_email_format");
	}

	public static UserValidationException requiredPassword() {
		return new UserValidationException(REQUIRED_PASSWORD, "required_password");
	}

	public static UserValidationException requiredPasswordConfirm() {
		return new UserValidationException(REQUIRED_PASSWORD_CONFIRM, "required_password_confirm");
	}

	public static UserValidationException invalidPasswordFormat() {
		return new UserValidationException(INVALID_PASSWORD_FORMAT,
			"invalid_password_format");
	}

	public static UserValidationException invalidPasswordLength() {
		return new UserValidationException(INVALID_PASSWORD_LENGTH,
			"invalid_password_length");
	}

	public static UserValidationException requiredNickname() {
		return new UserValidationException(REQUIRED_NICKNAME, "required_nickname");
	}

	public static UserValidationException invalidNicknameLength() {
		return new UserValidationException(INVALID_NICKNAME_LENGTH, "invalid_nickname_length");
	}

	public static UserValidationException passwordsDoNotMatch() {
		return new UserValidationException(PASSWORDS_DO_NOT_MATCH, "passwords_do_not_match");
	}
}
