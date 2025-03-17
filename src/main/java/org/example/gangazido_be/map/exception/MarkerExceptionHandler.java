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

@RestControllerAdvice // 모든 컨트롤러에서 발생하는 예외를 처리할 수 있도록 설정
public class MarkerExceptionHandler {

	// DTO @Valid 검증 실패 시 (400 Bad Request)
	@ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("message", "invalid_request");

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            if ("latitude".equals(error.getField()) || "longitude".equals(error.getField())) {
                errorResponse.put("message", "invalid_latitude_longitude");
            } else {
                errors.put(error.getField(), error.getDefaultMessage());
            }
        }

        errorResponse.put("data", errors.isEmpty() ? new HashMap<>() : errors);
        return ResponseEntity.badRequest().body(errorResponse);
    }

	// 위도 또는 경도 값이 null인 경우 (400 Bad Request)
	@ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidLatitudeLongitude(IllegalStateException ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "invalid_latitude_longitude");
        response.put("data", new HashMap<>());
        return ResponseEntity.badRequest().body(response);
    }
	@ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedException(SecurityException ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "required_authorization");
        response.put("data", new HashMap<>());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
	@ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("message", "internal_server_error");
		response.put("data", new HashMap<>());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}
}
