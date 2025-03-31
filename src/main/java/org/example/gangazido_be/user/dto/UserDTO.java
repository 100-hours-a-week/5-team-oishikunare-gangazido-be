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

	@NotEmpty(message = "required_password_confirm")
	@JsonProperty("user_password_confirm")
	private String passwordConfirm;

	@NotEmpty(message = "required_nickname")
	@Size(min = 2, max = 20, message = "invalid_nickname_length")
	@JsonProperty("user_nickname")
	private String nickname;

	// 기존 파일 업로드용 필드 (하위 호환성 유지)
	@JsonProperty("user_profileImage")
	private MultipartFile profileImage;

	// S3 프로필 이미지용 필드
	@JsonProperty("profile_image_url")
	private String profileImageUrl;

	// S3 객체 키 (업로드 시 생성된 파일 키)
	@JsonProperty("profile_image_key")
	private String profileImageKey;

	// 추가 유효성 검증 메서드 (필요시 사용)
	public boolean hasProfileImage() {
		return (profileImage != null && !profileImage.isEmpty()) ||
			(profileImageKey != null && !profileImageKey.isEmpty());
	}

	public boolean isPasswordMatching() {
		return password != null && password.equals(passwordConfirm);
	}
}
