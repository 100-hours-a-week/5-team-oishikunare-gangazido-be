package org.example.gangazido_be.user.util;

public class ApiMessages {
	// 성공 메시지
	public static final String SUCCESS = "요청이 성공적으로 처리되었습니다.";
	public static final String CREATED = "리소스가 성공적으로 생성되었습니다.";
	public static final String UPDATED = "리소스가 성공적으로 업데이트되었습니다.";
	public static final String DELETED = "리소스가 성공적으로 삭제되었습니다.";

	// 사용자 관련 메시지
	public static final String USER_CREATED = "회원가입이 성공적으로 완료되었습니다.";
	public static final String USER_UPDATED = "사용자 정보가 성공적으로 업데이트되었습니다.";
	public static final String USER_DELETED = "회원 탈퇴가 완료되었습니다.";
	public static final String LOGIN_SUCCESS = "로그인이 성공적으로 완료되었습니다.";
	public static final String LOGOUT_SUCCESS = "로그아웃이 성공적으로 완료되었습니다.";
	public static final String PASSWORD_CHANGED = "비밀번호가 성공적으로 변경되었습니다.";
	public static final String PROFILE_UPDATED = "프로필이 성공적으로 업데이트되었습니다.";

	// 에러 메시지
	public static final String BAD_REQUEST = "요청 형식이 올바르지 않습니다.";
	public static final String UNAUTHORIZED = "인증이 필요합니다.";
	public static final String FORBIDDEN = "접근 권한이 없습니다.";
	public static final String NOT_FOUND = "요청한 리소스를 찾을 수 없습니다.";
	public static final String INTERNAL_ERROR = "서버 내부 오류가 발생했습니다.";

	// 필드 검증 메시지
	public static final String VALIDATION_ERROR = "입력값 검증에 실패했습니다.";
}
