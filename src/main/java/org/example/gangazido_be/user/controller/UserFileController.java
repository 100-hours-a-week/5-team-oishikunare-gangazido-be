package org.example.gangazido_be.user.controller;

import org.example.gangazido_be.user.dto.UserApiResponse;
import org.example.gangazido_be.user.entity.User;
import org.example.gangazido_be.user.service.UserS3FileService;
import org.example.gangazido_be.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/v1/users")
public class UserFileController {

	private final UserS3FileService userS3FileService;
	private final UserService userService;
	private final Logger logger = LoggerFactory.getLogger(UserFileController.class);

	@Autowired
	public UserFileController(UserS3FileService userS3FileService, UserService userService) {
		this.userS3FileService = userS3FileService;
		this.userService = userService;
	}

	/**
	 * 프로필 이미지 업로드용 presigned URL 생성 API
	 *
	 * @param fileInfo 파일 정보 (확장자, MIME 타입)
	 * @param session 세션 객체
	 * @return presigned URL과 fileKey
	 */
	@PostMapping("/profile-image-upload-url")
	public ResponseEntity<UserApiResponse<Map<String, String>>> getProfileImageUploadUrl(
		@RequestBody Map<String, String> fileInfo,
		HttpSession session) {

		User user = (User) session.getAttribute("user");
		if (user == null) {
			return UserApiResponse.unauthorized("unauthorized");
		}

		try {
			String fileExtension = fileInfo.get("fileExtension");
			String contentType = fileInfo.get("contentType");

			// 파일 확장자 검증
			if (fileExtension == null || !fileExtension.matches("\\.(jpg|jpeg|png|gif)$")) {
				return UserApiResponse.badRequest("invalid_file_extension");
			}

			// MIME 타입 검증
			if (contentType == null || !contentType.startsWith("image/")) {
				return UserApiResponse.badRequest("invalid_content_type");
			}

			Map<String, String> presignedData = userS3FileService.generatePresignedUrlForProfileImage(
				fileExtension, contentType);

			return UserApiResponse.success("presigned_url_generated", presignedData);
		} catch (Exception e) {
			logger.error("Presigned URL 생성 오류: ", e);
			return UserApiResponse.internalError("internal_server_error");
		}
	}

	// @PostMapping("/profile-image-update")
	// public ResponseEntity<UserApiResponse<Map<String, Object>>> updateProfileImage(
	// 	@RequestBody Map<String, String> imageInfo,
	// 	HttpSession session) {
	//
	// 	User user = (User) session.getAttribute("user");
	// 	if (user == null) {
	// 		return UserApiResponse.unauthorized("unauthorized");
	// 	}
	//
	// 	try {
	// 		String fileKey = imageInfo.get("fileKey");
	//
	// 		// fileKey가 존재하는 경우에만 S3 처리 수행
	// 		if (fileKey != null && !fileKey.isEmpty()) {
	// 			// 파일이 S3에 실제로 업로드되었는지 확인
	// 			if (!userS3FileService.doesObjectExist(fileKey)) {
	// 				return UserApiResponse.badRequest("image_upload_incomplete");
	// 			}
	// 		}
	//
	// 		// 사용자 프로필 이미지 정보 업데이트 (null인 경우에도 처리)
	// 		// fileKey를 직접 저장 - URL 생성 로직 제거
	// 		User updatedUser = userService.updateProfileImage(user.getId(), fileKey);
	// 		session.setAttribute("user", updatedUser);
	//
	// 		Map<String, Object> responseData = new HashMap<>();
	//
	// 		// CloudFront URL로 변환하여 응답
	// 		if (updatedUser.getProfileImage() != null && !updatedUser.getProfileImage().isEmpty()) {
	// 			responseData.put("profileImage", userService.getCloudFrontUrlFromKey(updatedUser.getProfileImage()));
	// 		} else {
	// 			responseData.put("profileImage", null);
	// 		}
	//
	// 		return UserApiResponse.success("profile_image_updated", responseData);
	// 	} catch (Exception e) {
	// 		logger.error("프로필 이미지 업데이트 오류: ", e);
	// 		return UserApiResponse.internalError("internal_server_error");
	// 	}
	// }
}
