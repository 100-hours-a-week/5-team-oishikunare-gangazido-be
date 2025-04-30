package org.example.gangazido_be.user.exception;

import org.example.gangazido_be.user.dto.UserApiResponse;
import org.example.gangazido_be.user.util.UserApiMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice(basePackages = "org.example.gangazido_be.user")
public class UserExceptionHandler {
	private final Logger logger = LoggerFactory.getLogger(UserExceptionHandler.class);

	/**
	 * 사용자 관련 예외 처리
	 */
	@ExceptionHandler(UserException.class)
	public ResponseEntity<UserApiResponse<Object>> handleUserException(UserException e) {
		logger.warn("사용자 예외 발생: {}", e.getMessage());
		return UserApiResponse.badRequest(e.getErrorCode());
	}

	/**
	 * Bean Validation 예외 처리
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<UserApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException e) {
		FieldError fieldError = e.getBindingResult().getFieldError();
		String errorCode = "validation_error";

		if (fieldError != null) {
			errorCode = fieldError.getDefaultMessage();
		}

		logger.warn("유효성 검증 실패: {}", errorCode);
		return UserApiResponse.badRequest(errorCode);
	}

	/**
	 * 필수 요청 파라미터 누락 예외 처리
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<UserApiResponse<Object>> handleMissingParameterException(
		MissingServletRequestParameterException e) {
		String paramName = e.getParameterName();
		String errorCode;

		// 파라미터별 에러 코드 매핑
		switch (paramName) {
			case "email":
				errorCode = "required_email";
				break;
			case "password":
				errorCode = "required_password";
				break;
			case "nickname":
				errorCode = "required_nickname";
				break;
			default:
				errorCode = "required_field";
		}

		logger.warn("필수 파라미터 누락: {}", paramName);
		return UserApiResponse.badRequest(errorCode);
	}

	/**
	 * 기타 모든 예외 처리
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<UserApiResponse<Object>> handleException(Exception e) {
		logger.error("서버 오류 발생: ", e);
		return UserApiResponse.internalError(UserApiMessages.INTERNAL_ERROR);
	}

	// 제리 추가
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<?> handleNoResourceFound(NoResourceFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("정적 리소스를 찾을 수 없습니다.");
	}
}
