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
public class UserPasswordChangeRequestDTO {
	@NotEmpty(message = "required_current_password")
	@JsonProperty("current_password")
	private String currentPassword;

	@NotEmpty(message = "required_new_password")
	@JsonProperty("new_password")
	private String newPassword;

	@NotEmpty(message = "required_confirm_password")
	@JsonProperty("confirm_password")
	private String confirmPassword;
}
