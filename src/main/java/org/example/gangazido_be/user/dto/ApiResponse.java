package org.example.gangazido_be.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
	private String message;
	private T data;

	// 성공 응답 생성 (200 OK)
	public static <T> ResponseEntity<ApiResponse<T>> success(String message, T data) {
		return ResponseEntity.ok(
			ApiResponse.<T>builder()
				.message(message)
				.data(data)
				.build()
		);
	}

	// 에러 응답 생성 (커스텀 HTTP 상태 코드)
	public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(
			ApiResponse.<T>builder()
				.message(message)
				.data(null)
				.build()
		);
	}

	// 에러 응답 생성 (커스텀 HTTP 상태 코드, 데이터 포함)
	public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message, T data) {
		return ResponseEntity.status(status).body(
			ApiResponse.<T>builder()
				.message(message)
				.data(data)
				.build()
		);
	}

	// 자주 사용하는 응답 패턴들을 미리 정의
	public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
		return ResponseEntity.status(HttpStatus.CREATED).body(
			ApiResponse.<T>builder()
				.message(message)
				.data(data)
				.build()
		);
	}

	public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
		return error(HttpStatus.BAD_REQUEST, message);
	}

	public static <T> ResponseEntity<ApiResponse<T>> unauthorized(String message) {
		return error(HttpStatus.UNAUTHORIZED, message);
	}

	public static <T> ResponseEntity<ApiResponse<T>> forbidden(String message) {
		return error(HttpStatus.FORBIDDEN, message);
	}

	public static <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
		return error(HttpStatus.NOT_FOUND, message);
	}

	public static <T> ResponseEntity<ApiResponse<T>> internalError(String message) {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, message);
	}
}
