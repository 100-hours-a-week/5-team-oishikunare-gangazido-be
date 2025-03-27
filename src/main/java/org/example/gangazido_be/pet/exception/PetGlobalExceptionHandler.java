package org.example.gangazido_be.pet.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice("org.example.gangazido_be.pet")
public class PetGlobalExceptionHandler {

	private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("message", message);
		response.put("data", null);
		return new ResponseEntity<>(response, status);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
		String message = (ex.getBindingResult().hasErrors() && ex.getBindingResult().getFieldError() != null) ?
			ex.getBindingResult().getFieldError().getDefaultMessage() :
			"invalid_request";
		return buildResponse(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(PetException.class)
	public ResponseEntity<Map<String, Object>> handlePetException(PetException ex) {
		return buildResponse(ex.getStatus(), ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex, HttpServletRequest request) {
		String uri = request.getRequestURI();
		if (uri.startsWith("/actuator")) {
			// actuator 요청은 스프링의 기본 예외 처리로 넘긴다
			throw new RuntimeException(ex);
		}
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error");
	}
}
