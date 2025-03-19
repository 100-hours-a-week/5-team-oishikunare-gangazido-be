package org.example.gangazido_be.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeRequestDTO {
	@NotEmpty(message = "현재 비밀번호는 필수 입력 항목입니다.")
	@JsonProperty("current_password")
	private String currentPassword;

	@NotEmpty(message = "새 비밀번호는 필수 입력 항목입니다.")
	@JsonProperty("new_password")
	private String newPassword;

	@NotEmpty(message = "비밀번호 확인은 필수 입력 항목입니다.")
	@JsonProperty("confirm_password")
	private String confirmPassword;
}
