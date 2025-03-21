package org.example.gangazido_be.pet.common;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"message", "data"})
public class PetApiResponse<T> {
	private final String message;
	private final T data;

	private PetApiResponse(String message, T data) {
		this.message = message;
		this.data = data;
	}

	// ✅ 성공 응답 생성 (제네릭 적용)
	public static <T> PetApiResponse<T> of(String message, T data) {
		return new PetApiResponse<>(message, data);
	}

	// ✅ 오류 응답 생성
	public static <T> PetApiResponse<T> error(String message) {
		return new PetApiResponse<>(message, null);
	}
}
