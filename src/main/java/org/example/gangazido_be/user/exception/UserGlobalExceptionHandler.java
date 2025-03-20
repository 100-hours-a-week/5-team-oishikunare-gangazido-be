package org.example.gangazido_be.user.exception;

import org.example.gangazido_be.user.dto.UserApiResponse;
import org.example.gangazido_be.user.util.UserApiMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "org.example.gangazido_be.user.controller")
public class UserGlobalExceptionHandler {
	private final Logger logger = LoggerFactory.getLogger(UserGlobalExceptionHandler.class);

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<UserApiResponse<Object>> handleRuntimeException(RuntimeException e) {
		logger.warn("Runtime error occurred: {}", e.getMessage());
		return UserApiResponse.badRequest(e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<UserApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});
		logger.warn("Validation failed: {}", errors);
		return UserApiResponse.error(HttpStatus.BAD_REQUEST, UserApiMessages.VALIDATION_ERROR, errors);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<UserApiResponse<Object>> handleException(Exception e) {
		logger.error("Server error occurred:", e);
		return UserApiResponse.internalError(UserApiMessages.INTERNAL_ERROR);
	}
}
