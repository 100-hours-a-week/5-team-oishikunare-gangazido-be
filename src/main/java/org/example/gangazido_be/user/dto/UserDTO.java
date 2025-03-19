package org.example.gangazido_be.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.*;
import lombok.*;

import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
	@NotEmpty(message = "이메일은 필수 입력 항목입니다.")
	@Email(message = "유효한 이메일 형식이 아닙니다.")
	@JsonProperty("user_email")
	private String email;

	@NotEmpty(message = "비밀번호는 필수 입력 항목입니다.")
	@Size(min = 8, max = 20, message = "비밀번호는 8자 이상, 20자 이하여야 합니다.")
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
		message = "비밀번호는 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다.")
	@JsonProperty("user_password")
	private String password;

	@NotEmpty(message = "닉네임은 필수 입력 항목입니다.")
	@JsonProperty("user_nickname")
	private String nickname;

	@JsonProperty("user_profileImage")
	private MultipartFile profileImage;
}
