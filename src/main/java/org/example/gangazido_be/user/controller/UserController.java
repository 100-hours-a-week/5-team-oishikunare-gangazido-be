package org.example.gangazido_be.user.controller;

import org.example.gangazido_be.user.dto.UserApiResponse;
import org.example.gangazido_be.user.dto.UserLoginRequestDTO;
import org.example.gangazido_be.user.dto.UserPasswordChangeRequestDTO;
import org.example.gangazido_be.user.dto.UserDTO;
import org.example.gangazido_be.user.entity.User;
import org.example.gangazido_be.user.service.UserService;
import org.example.gangazido_be.user.util.UserApiMessages;
import org.example.gangazido_be.user.util.UserIdEncryptionUtil;
import org.example.gangazido_be.user.validator.UserPasswordValidator;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;
import java.util.Date;

@RestController
@RequestMapping("/v1/users")
public class UserController {
	private final UserService userService;
	private final UserIdEncryptionUtil idEncryptionUtil;
	private final Logger logger = LoggerFactory.getLogger(UserController.class);

	@Autowired
	public UserController(UserService userService, UserIdEncryptionUtil idEncryptionUtil) {
		this.userService = userService;
		this.idEncryptionUtil = idEncryptionUtil;
	}

	@PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<UserApiResponse<Map<String, Object>>> registerUser(
		@RequestPart("user_email") String email,
		@RequestPart("user_password") String password,
		@RequestPart("user_password_confirm") String passwordConfirm,
		@RequestPart("user_nickname") String nickname,
		@RequestPart(value = "user_profileImage", required = false) MultipartFile profileImage,
		HttpSession session,
		HttpServletResponse response) {
		try {
			// 입력값 검증
			if (email == null || email.isEmpty()) {
				return UserApiResponse.badRequest("required_email");
			}

			// 이메일 형식 검증
			if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
				return UserApiResponse.badRequest("invalid_email_format");
			}

			if (password == null || password.isEmpty()) {
				return UserApiResponse.badRequest("required_password");
			}

			if (nickname == null || nickname.isEmpty()) {
				return UserApiResponse.badRequest("required_nickname");
			}

			// 닉네임 길이 검증
			if (nickname.length() < 2 || nickname.length() > 20) {
				return UserApiResponse.badRequest("invalid_nickname_length");
			}

			// 비밀번호 확인 처리 추가
			if (passwordConfirm == null || passwordConfirm.isEmpty()) {
				return UserApiResponse.badRequest("required_password_confirm");
			}

			// 비밀번호 확인 유효성 검사
			if (!UserPasswordValidator.isValid(passwordConfirm)) {
				return UserApiResponse.badRequest("invalid_password_confirm");
			}

			// 비밀번호 복잡성 검증
			if (!UserPasswordValidator.isValid(password)) {
				return UserApiResponse.badRequest(UserPasswordValidator.getValidationMessage());
			}

			// DTO 생성
			UserDTO userDTO = UserDTO.builder()
				.email(email)
				.password(password)
				.nickname(nickname)
				.profileImage(profileImage)
				.build();

			// 사용자 등록
			User registeredUser = userService.registerUser(userDTO);

			// 세션 및 쿠키 설정
			setSessionAndCookie(registeredUser, session, response);

			// 사용자 ID를 암호화하여 응답
			String encryptedId = idEncryptionUtil.encrypt(registeredUser.getId());

			Map<String, Object> responseData = new HashMap<>();
			responseData.put("userId", encryptedId);
			responseData.put("nickname", registeredUser.getNickname());

			return UserApiResponse.success(UserApiMessages.USER_CREATED, responseData);
		} catch (RuntimeException e) {
			logger.warn("회원가입 실패: {}", e.getMessage());

			// 예외 메시지에 따른 처리
			String errorMessage = e.getMessage();

			if ("duplicate_email".equals(errorMessage)) {
				return UserApiResponse.badRequest("duplicate_email");
			} else if ("duplicate_nickname".equals(errorMessage)) {
				return UserApiResponse.badRequest("duplicate_nickname");
			} else if (errorMessage != null && errorMessage.contains("Data too long for column 'nickname'")) {
				return UserApiResponse.badRequest("invalid_nickname_length");
			} else if ("invalid_password_format".equals(errorMessage)) {
				return UserApiResponse.badRequest("invalid_password_format");
			} else {
				return UserApiResponse.badRequest(errorMessage);
			}
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return UserApiResponse.internalError(UserApiMessages.INTERNAL_ERROR);
		}
	}

	@PostMapping("/login")
	public ResponseEntity<UserApiResponse<Map<String, Object>>> login(
		@Valid @RequestBody UserLoginRequestDTO loginRequest,
		HttpSession session,
		HttpServletResponse response) {
		try {
			// 입력값 유효성 검사
			if (loginRequest.getEmail() == null || loginRequest.getEmail().isEmpty()) {
				return UserApiResponse.badRequest("required_email");
			}

			if (loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
				return UserApiResponse.badRequest("required_password");
			}

			User user = userService.login(loginRequest.getEmail(), loginRequest.getPassword());

			// 세션 및 쿠키 설정
			setSessionAndCookie(user, session, response);

			// 사용자 ID를 암호화하여 응답
			String encryptedId = idEncryptionUtil.encrypt(user.getId());

			Map<String, Object> responseData = new HashMap<>();
			responseData.put("userId", encryptedId);
			responseData.put("nickname", user.getNickname());

			return UserApiResponse.success(UserApiMessages.LOGIN_SUCCESS, responseData);
		} catch (RuntimeException e) {
			String errorMessage = e.getMessage();

			// 모든 비밀번호/이메일 관련 오류는 통일된 메시지 사용
			if ("missing_email".equals(errorMessage) || "invalid_password".equals(errorMessage)) {
				return UserApiResponse.badRequest("invalid_email_or_password");
			} else {
				logger.warn("로그인 실패: {}", e.getMessage());
				return UserApiResponse.badRequest("invalid_email_or_password");
			}
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return UserApiResponse.internalError("internal_server_error");
		}
	}

	// 사용자 정보 확인 API
	@GetMapping("/me")
	public ResponseEntity<UserApiResponse<Map<String, Object>>> getCurrentUser(HttpSession session) {
		User user = (User) session.getAttribute("user");

		if (user == null) {
			return UserApiResponse.unauthorized(UserApiMessages.UNAUTHORIZED);
		}

		Map<String, Object> responseData = new HashMap<>();
		responseData.put("userId", idEncryptionUtil.encrypt(user.getId()));
		responseData.put("email", user.getEmail());
		responseData.put("nickname", user.getNickname());
		responseData.put("profileImage", user.getProfileImage());

		return UserApiResponse.success(UserApiMessages.SUCCESS, responseData);
	}

	@PostMapping("/logout")
	public ResponseEntity<UserApiResponse<Object>> logout(
		HttpServletRequest request,
		HttpServletResponse response,
		HttpSession session) {
		try {
			// 세션 무효화
			session.invalidate();

			// 쿠키 삭제
			Cookie[] cookies = request.getCookies();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					if (cookie.getName().equals("SESSIONID")) {
						cookie.setValue("");
						cookie.setPath("/");
						cookie.setMaxAge(0);
						response.addCookie(cookie);
						break;
					}
				}
			}

			return UserApiResponse.success(UserApiMessages.LOGOUT_SUCCESS, null);
		} catch (Exception e) {
			logger.error("로그아웃 처리 중 오류: ", e);
			return UserApiResponse.internalError(UserApiMessages.INTERNAL_ERROR);
		}
	}

	// 중복 이메일 확인 API
	@GetMapping("/check-email")
	public ResponseEntity<UserApiResponse<Map<String, Boolean>>> checkEmailDuplicate(@RequestParam String email) {
		boolean isDuplicate = userService.isEmailDuplicate(email);
		Map<String, Boolean> responseData = Map.of("isDuplicate", isDuplicate);
		return UserApiResponse.success(UserApiMessages.SUCCESS, responseData);
	}

	// 중복 닉네임 확인 API
	@GetMapping("/check-nickname")
	public ResponseEntity<UserApiResponse<Map<String, Boolean>>> checkNicknameDuplicate(@RequestParam String nickname) {
		boolean isDuplicate = userService.isNicknameDuplicate(nickname);
		logger.info("닉네임 중복 체크: {}, 결과: {}", nickname, isDuplicate);
		Map<String, Boolean> responseData = Map.of("isDuplicate", isDuplicate);
		return UserApiResponse.success(UserApiMessages.SUCCESS, responseData);
	}

	@PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<UserApiResponse<Map<String, Object>>> updateMyInfo(
		@RequestPart(value = "user_nickname", required = false) String nickname,
		@RequestPart(value = "user_profile_image", required = false) MultipartFile profileImage,
		HttpSession session) {

		User user = (User) session.getAttribute("user");
		if (user == null) {
			return UserApiResponse.unauthorized(UserApiMessages.UNAUTHORIZED);
		}

		try {
			// 닉네임과 프로필 이미지가 둘 다 제공되지 않은 경우 처리
			if ((nickname == null || nickname.isEmpty()) &&
				(profileImage == null || profileImage.isEmpty())) {
				return UserApiResponse.badRequest("profile_update_data_required");
			}

			// 닉네임 유효성 검사
			if (nickname != null && !nickname.isEmpty()) {
				// 제어 문자 제거
				nickname = nickname.replaceAll("[\\p{Cntrl}]", "");

				// 빈 문자열이 되면 처리
				if (nickname.isEmpty()) {
					return UserApiResponse.badRequest("valid_nickname_required");
				}

				// 길이 제한 검사
				if (nickname.length() < 2 || nickname.length() > 20) {
					return UserApiResponse.badRequest("invalid_nickname_length");
				}

				// 닉네임 중복 검사 (현재 사용자의 닉네임은 제외)
				if (!nickname.equals(user.getNickname()) && userService.isNicknameDuplicate(nickname)) {
					return UserApiResponse.badRequest("duplicate_nickname");
				}
			}

			// 프로필 이미지 파일 크기 검사 (예: 5MB 제한)
			if (profileImage != null && !profileImage.isEmpty() && profileImage.getSize() > 5 * 1024 * 1024) {
				return UserApiResponse.badRequest("profile_image_too_large");
			}

			User updatedUser = userService.updateUserInfo(user.getId(), nickname, profileImage);
			session.setAttribute("user", updatedUser); // 세션 업데이트

			Map<String, Object> responseData = new HashMap<>();
			responseData.put("userId", idEncryptionUtil.encrypt(updatedUser.getId()));
			responseData.put("nickname", updatedUser.getNickname());
			responseData.put("profileImage", updatedUser.getProfileImage());

			return UserApiResponse.success("update_user_data_success", responseData);
		} catch (RuntimeException e) {
			logger.warn("사용자 정보 업데이트 실패: {}", e.getMessage());

			// 구체적인 예외 메시지 처리
			String errorMessage = e.getMessage();

			if ("duplicate_nickname".equals(errorMessage)) {
				return UserApiResponse.badRequest("duplicate_nickname");
			} else if ("invalid_nickname_format".equals(errorMessage)) {
				return UserApiResponse.badRequest("invalid_nickname_format");
			} else if ("invalid_nickname_length".equals(errorMessage)) {
				return UserApiResponse.badRequest("invalid_nickname_length");
			} else if (errorMessage != null && errorMessage.contains("Data too long for column 'nickname'")) {
				return UserApiResponse.badRequest("invalid_nickname_length");
			} else {
				return UserApiResponse.badRequest(errorMessage);
			}
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return UserApiResponse.internalError(UserApiMessages.INTERNAL_ERROR);
		}
	}

	@DeleteMapping("/me")
	public ResponseEntity<UserApiResponse<Object>> deleteMyAccount(
		HttpServletRequest request,
		HttpServletResponse response,
		HttpSession session) {

		User user = (User) session.getAttribute("user");
		if (user == null) {
			return UserApiResponse.unauthorized(UserApiMessages.UNAUTHORIZED);
		}

		try {
			// 회원 탈퇴 처리
			userService.deleteUser(user.getId());

			// 세션 무효화
			session.invalidate();

			// 쿠키 삭제
			Cookie[] cookies = request.getCookies();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					if (cookie.getName().equals("JSESSIONID")) {
						cookie.setValue("");
						cookie.setPath("/");
						cookie.setMaxAge(0);
						response.addCookie(cookie);
						break;
					}
				}
			}

			return UserApiResponse.success(UserApiMessages.USER_DELETED, null);
		} catch (RuntimeException e) {
			logger.warn("회원 탈퇴 실패: {}", e.getMessage());
			return UserApiResponse.badRequest(e.getMessage());
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return UserApiResponse.internalError(UserApiMessages.INTERNAL_ERROR);
		}
	}

	@PatchMapping("/me/password")
	public ResponseEntity<UserApiResponse<Object>> changePassword(
		@Valid @RequestBody UserPasswordChangeRequestDTO requestDTO,
		HttpSession session) {

		User user = (User) session.getAttribute("user");
		if (user == null) {
			return UserApiResponse.unauthorized("required_authorization");
		}

		try {
			// 현재 비밀번호 검증
			if (requestDTO.getCurrentPassword() == null || requestDTO.getCurrentPassword().isEmpty()) {
				return UserApiResponse.badRequest("required_current_password");
			}

			// 새 비밀번호 검증
			if (requestDTO.getNewPassword() == null || requestDTO.getNewPassword().isEmpty()) {
				return UserApiResponse.badRequest("required_new_password"); // 수정
			}

			// 비밀번호 확인 메시지 처리 추가
			if (!UserPasswordValidator.isValid(requestDTO.getNewPassword())) {
				return UserApiResponse.badRequest("invalid_new_password_format");
			}

			if (requestDTO.getNewPassword().length() < 8 || requestDTO.getNewPassword().length() > 20) {
				return UserApiResponse.badRequest("invalid_new_password_length");
			}

			if (requestDTO.getConfirmPassword() == null || requestDTO.getConfirmPassword().isEmpty()) {
				return UserApiResponse.badRequest("required_new_password_confirm");
			}

			if (!UserPasswordValidator.isValid(requestDTO.getConfirmPassword())) {
				return UserApiResponse.badRequest("invalid_new_password_confirm_format");
			}

			if (requestDTO.getConfirmPassword().length() < 8 || requestDTO.getConfirmPassword().length() > 20) {
				return UserApiResponse.badRequest("invalid_new_password_confirm_length");
			}

			// 새 비밀번호와 확인 비밀번호 일치 확인
			if (!requestDTO.getNewPassword().equals(requestDTO.getConfirmPassword())) {
				return UserApiResponse.badRequest("passwords_do_not_match");
			}

			// 비밀번호 복잡성 검증
			if (!UserPasswordValidator.isValid(requestDTO.getNewPassword())) {
				return UserApiResponse.badRequest("invalid_new_password_format");
			}

			User updatedUser = userService.changePassword(
				user.getId(),
				requestDTO.getCurrentPassword(),
				requestDTO.getNewPassword()
			);

			session.setAttribute("user", updatedUser); // 세션 업데이트

			return UserApiResponse.success(UserApiMessages.PASSWORD_CHANGED, null);
		} catch (RuntimeException e) {
			String errorMessage = e.getMessage();

			if ("invalid_current_password".equals(errorMessage)) {
				return UserApiResponse.badRequest("invalid_current_password");
			} else if ("missing_user".equals(errorMessage)) {
				return UserApiResponse.badRequest("user_not_found");
			} else if (errorMessage.startsWith("invalid_password_")) {
				return UserApiResponse.badRequest(errorMessage);
			} else {
				logger.warn("비밀번호 변경 실패: {}", errorMessage);
				return UserApiResponse.badRequest(errorMessage);
			}
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return UserApiResponse.internalError("internal_server_error");
		}
	}

	// 공통 세션 및 쿠키 설정 메서드
	private void setSessionAndCookie(User user, HttpSession session, HttpServletResponse response) {
		// 세션에 사용자 정보 저장
		session.setAttribute("user", user);
		session.setMaxInactiveInterval(3600); // 세션 유효시간 1시간

		// 쿠키 설정 (세션 ID 저장)
		Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
		sessionCookie.setPath("/");
		sessionCookie.setHttpOnly(true);
		sessionCookie.setSecure(false); // HTTPS 환경에서만 사용 해제
		sessionCookie.setMaxAge(3600); // 1시간 유효
		response.addCookie(sessionCookie);

		// 디버깅 로그 추가
		logger.debug("쿠키 설정 완료: {}, 세션 ID: {}", sessionCookie.getName(), session.getId());
	}
}
