package org.example.gangazido_be.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {
	@NotEmpty(message = "이메일은 필수 입력 항목입니다.")
	@Email(message = "유효한 이메일 형식이 아닙니다.")
	@JsonProperty("user_email")
	private String email;

	@NotEmpty(message = "비밀번호는 필수 입력 항목입니다.")
	@JsonProperty("user_password")
	private String password;
}
