package org.example.gangazido_be.user.validator;

import java.util.regex.Pattern;

public class PasswordValidator {
	private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
	private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
	private static final Pattern HAS_NUMBER = Pattern.compile("\\d");
	private static final Pattern HAS_SPECIAL_CHAR = Pattern.compile("[@$!%*?&]");

	public static boolean isValid(String password) {
		if (password == null || password.length() < 8 || password.length() > 20) {
			return false;
		}

		return HAS_UPPERCASE.matcher(password).find() &&
			HAS_LOWERCASE.matcher(password).find() &&
			HAS_NUMBER.matcher(password).find() &&
			HAS_SPECIAL_CHAR.matcher(password).find();
	}

	public static String getValidationMessage() {
		return "invalid_password_format";
	}
}
