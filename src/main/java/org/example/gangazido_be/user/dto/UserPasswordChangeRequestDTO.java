package org.example.gangazido_be.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
	@Size(min = 8, max = 20, message = "invalid_new_password_length")
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
		message = "invalid_new_password_format")
	@JsonProperty("new_password")
	private String newPassword;

	@NotEmpty(message = "required_confirm_password")
	@Size(min = 8, max = 20, message = "invalid_new_password_confirm_length")
	@JsonProperty("confirm_password")
	private String confirmPassword;
}
