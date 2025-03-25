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
public class UserLoginRequestDTO {
	@NotEmpty(message = "required_email")
	@Email(message = "invalid_email_format")
	@JsonProperty("user_email")
	private String email;

	@NotEmpty(message = "required_password")
	@JsonProperty("user_password")
	private String password;
}
