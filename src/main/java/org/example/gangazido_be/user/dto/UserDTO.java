package org.example.gangazido_be.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.*;
import lombok.*;

import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserDTO {
	@NotEmpty(message = "required_email")
	@Email(message = "invalid_email_format")
	@JsonProperty("user_email")
	private String email;

	@NotEmpty(message = "required_password")
	@Size(min = 8, max = 20, message = "invalid_password_length")
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
		message = "invalid_password_format")
	@JsonProperty("user_password")
	private String password;

	@NotEmpty(message = "required_password")
	@JsonProperty("user_password_confirm")
	private String passwordConfirm;

	@NotEmpty(message = "required_nickname")
	@JsonProperty("user_nickname")
	private String nickname;

	@JsonProperty("user_profileImage")
	private MultipartFile profileImage;
}
