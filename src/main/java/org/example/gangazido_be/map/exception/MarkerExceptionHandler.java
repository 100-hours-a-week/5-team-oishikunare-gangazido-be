package org.example.gangazido_be.map.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice("org.example.gangazido_be.map") // 모든 컨트롤러에서 발생하는 예외를 처리할 수 있도록 설정
public class MarkerExceptionHandler {

	// DTO @Valid 검증 실패 시 (400 Bad Request)
	// DTO @NotNull 등의 에러, DTO 검증 통과하지 못한 필드를 찾아 메시지와 함께 응답을 반환함
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
		Map<String, Object> errorResponse = new LinkedHashMap<>();
		errorResponse.put("message", "invalid_request");    // 예외 발생 시 기본 응답 메시지를 invalid_request로 설정

		Map<String, String> errors = new HashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {    // 유효성 검사 실패한 필드 목록을 가져옴
			if ("latitude".equals(error.getField()) || "longitude".equals(error.getField())) {
				errorResponse.put("message", "invalid_latitude_longitude");
			} else {    // latitude, longitude 필드에서 오류 발생 시 메시지를 invalid_latitude_longitudefh 변경
				errors.put(error.getField(), error.getDefaultMessage());
			}    // 필드명 : 오류 메시지 형태로 errors 맵에 저장
		}

		errorResponse.put("data", errors.isEmpty() ? new HashMap<>() : errors);    // errors 맵이 비어있으면 data가 null이 아닌 JSON {} 되도록
		return ResponseEntity.badRequest().body(errorResponse);    // 400 상태코드 설정, body에 에러 응답 JSON으로 반환
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleNotFoundException(IllegalArgumentException ex) {
		Map<String, Object> response = new LinkedHashMap<>();
		if ("marker_not_found".equals(ex.getMessage())) {
			response.put("message", "marker_not_found");
			response.put("data", new HashMap<>());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response); // 404 반환
		}
		response.put("message", "invalid_marker_id");
		response.put("data", new HashMap<>());
		return ResponseEntity.badRequest().body(response);
	}

	// 위도 또는 경도 값이 null인 경우 (400 Bad Request)
	@ExceptionHandler(IllegalStateException.class)    // IllegalStateException이 발생시 이 메서드에서 처리
	public ResponseEntity<Map<String, Object>> handleInvalidLatitudeLongitude(IllegalStateException ex) {
		Map<String, Object> response = new LinkedHashMap<>();
		String message = ex.getMessage();

		if ("1시간에 최대 30개의 마커만 등록할 수 있습니다.".equals(message)) {
			response.put("message", "limit_exceeded");
			response.put("error", message);
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
		}

		response.put("message", "invalid_latitude_longitude");    // 위/경도 누락 응답 메시지
		response.put("data", new HashMap<>());    // 응답 data : {}
		return ResponseEntity.badRequest().body(response);    // 400 Bad Request 반환
	}

	@ExceptionHandler(SecurityException.class)
	public ResponseEntity<Map<String, Object>> handleUnauthorizedException(SecurityException ex) {
		Map<String, Object> response = new LinkedHashMap<>();
		if ("required_permission".equals(ex.getMessage())) {
			response.put("message", "required_permission");
			response.put("data", new HashMap<>());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response); // 403 반환
		}
		response.put("message", "required_authorization");
		response.put("data", new HashMap<>());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);    // 401
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("message", "internal_server_error");
		response.put("data", new HashMap<>());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);    // 500
	}
}
