package org.example.gangazido_be.pet.exception;

// 전역 예외 처리기

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
		Map<String, Object> response = new HashMap<>();
		response.put("data", null);
		response.put("message", message);
		return new ResponseEntity<>(response, status);
	}

	// DTO에서 @Valid로 발생한 예외 처리 (400)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
		String message = (ex.getBindingResult().hasErrors() && ex.getBindingResult().getFieldError() != null) ? ex.getBindingResult().getFieldError().getDefaultMessage() : "invalid_request";

		return buildResponse(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(PetNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handlePetNotFoundException(PetNotFoundException ex) {
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	// 비즈니스 로직에서 발생한 예외 처리 (400, 404 등)
	@ExceptionHandler(PetException.class)
	public ResponseEntity<Map<String, Object>> handlePetException(PetException ex) {
		return buildResponse(ex.getStatus(), ex.getMessage());
	}

	// 그 외 예상치 못한 예외 (500)
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex) {
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error");
	}
}
