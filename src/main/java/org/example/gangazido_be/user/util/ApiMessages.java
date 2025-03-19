package org.example.gangazido_be.user.util;

public class ApiMessages {
	// 성공 메시지
	public static final String SUCCESS = "request_success";
	public static final String CREATED = "resource_created";
	public static final String UPDATED = "resource_updated";
	public static final String DELETED = "resource_deleted";

	// 사용자 관련 메시지
	public static final String USER_CREATED = "register_success";
	public static final String USER_UPDATED = "update_user_data_success";
	public static final String USER_DELETED = "delete_user_success";
	public static final String LOGIN_SUCCESS = "login_success";
	public static final String LOGOUT_SUCCESS = "logout_success";
	public static final String PASSWORD_CHANGED = "change_user_password_success";
	public static final String PROFILE_UPDATED = "profile_updated";

	// 에러 메시지
	public static final String BAD_REQUEST = "invalid_request";
	public static final String UNAUTHORIZED = "required_authentication";
	public static final String FORBIDDEN = "access_denied";
	public static final String NOT_FOUND = "resource_not_found";
	public static final String INTERNAL_ERROR = "server_error";

	// 필드 검증 메시지
	public static final String VALIDATION_ERROR = "validation_failed";
}
