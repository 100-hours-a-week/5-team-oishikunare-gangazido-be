package org.example.gangazido_be.user.validator;

import java.util.regex.Pattern;

/**
 * 비밀번호 유효성 검증 유틸리티 클래스
 */
public class UserPasswordValidator {
	// 비밀번호 정규식 패턴: 8-20자, 최소 하나의 대문자, 소문자, 숫자, 특수문자 포함
	private static final String PASSWORD_PATTERN =
		"^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$";

	private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

	/**
	 * 비밀번호 유효성 검증
	 *
	 * @param password 검증할 비밀번호
	 * @return 유효성 여부
	 */
	public static boolean isValid(String password) {
		if (password == null) {
			return false;
		}
		return pattern.matcher(password).matches();
	}

	/**
	 * 비밀번호 유효성 검증 메시지 반환
	 *
	 * @return 검증 실패 시 에러 메시지
	 */
	public static String getValidationMessage() {
		return "비밀번호는 8-20자의 대소문자, 숫자, 특수문자(@$!%*?&)를 포함해야 합니다.";
	}

	/**
	 * 비밀번호 길이 검증
	 *
	 * @param password 검증할 비밀번호
	 * @return 길이 유효성 여부
	 */
	public static boolean isValidLength(String password) {
		return password != null && password.length() >= 8 && password.length() <= 20;
	}
}
