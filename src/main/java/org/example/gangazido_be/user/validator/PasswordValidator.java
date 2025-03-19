package org.example.gangazido_be.user.validator;

import java.util.regex.Pattern;

/**
 * 비밀번호의 유효성을 검사하는 유틸리티 클래스입니다.
 */
public class PasswordValidator {

	private static final int MIN_LENGTH = 8;
	private static final int MAX_LENGTH = 20;

	private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
	private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
	private static final Pattern HAS_NUMBER = Pattern.compile("\\d");
	private static final Pattern HAS_SPECIAL_CHAR = Pattern.compile("[@$!%*?&]");

	// 인스턴스 생성 방지
	private PasswordValidator() {
		throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}

	/**
	 * 비밀번호가 유효한 형식을 갖추었는지 검사합니다.
	 *
	 * @param password 검사할 비밀번호
	 * @return 모든 조건을 만족하면 true, 그렇지 않으면 false
	 */
	public static boolean isValid(String password) {
		if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
			return false;
		}

		return HAS_UPPERCASE.matcher(password).find() &&
			HAS_LOWERCASE.matcher(password).find() &&
			HAS_NUMBER.matcher(password).find() &&
			HAS_SPECIAL_CHAR.matcher(password).find();
	}

	/**
	 * 비밀번호 형식에 맞지 않을 때 사용할 메시지 키를 반환합니다.
	 *
	 * @return "invalid_password_format"
	 */
	public static String getValidationMessage() {
		return "invalid_password_format";
	}
}
