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
		@RequestPart("user_nickname") String nickname,
		@RequestPart(value = "user_profileImage", required = false) MultipartFile profileImage,
		HttpSession session,
		HttpServletResponse response) {
		try {
			// 입력값 검증
			if (email == null || email.isEmpty()) {
				return UserApiResponse.badRequest("email_required");
			}

			if (password == null || password.isEmpty()) {
				return UserApiResponse.badRequest("password_required");
			}

			if (nickname == null || nickname.isEmpty()) {
				return UserApiResponse.badRequest("nickname_required");
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
			return UserApiResponse.badRequest(e.getMessage());
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
			logger.warn("로그인 실패: {}", e.getMessage());
			return UserApiResponse.badRequest(e.getMessage());
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return UserApiResponse.internalError(UserApiMessages.INTERNAL_ERROR);
		}
	}

	// 로그인 상태 확인 API
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
		Map<String, Boolean> responseData = Map.of("isDuplicate", isDuplicate);
		return UserApiResponse.success(UserApiMessages.SUCCESS, responseData);
	}

	// 프로필 이미지 업데이트 API
	@PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<UserApiResponse<Map<String, Object>>> updateProfileImage(
		@RequestPart("profileImage") MultipartFile profileImage,
		HttpSession session) {

		User user = (User) session.getAttribute("user");
		if (user == null) {
			return UserApiResponse.unauthorized(UserApiMessages.UNAUTHORIZED);
		}

		try {
			User updatedUser = userService.updateProfileImage(user.getId(), profileImage);
			session.setAttribute("user", updatedUser); // 세션 업데이트

			Map<String, Object> responseData = Map.of("profileImage", updatedUser.getProfileImage());
			return UserApiResponse.success(UserApiMessages.PROFILE_UPDATED, responseData);
		} catch (Exception e) {
			logger.error("프로필 이미지 업데이트 중 오류: ", e);
			return UserApiResponse.internalError(UserApiMessages.INTERNAL_ERROR);
		}
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
			// 닉네임 정제 - 제어 문자 제거
			if (nickname != null && !nickname.isEmpty()) {
				nickname = nickname.replaceAll("[\\p{Cntrl}]", "");

				// 빈 문자열이 되면 처리
				if (nickname.isEmpty()) {
					return UserApiResponse.badRequest("valid_nickname_required");
				}
			}

			// 정보 업데이트 (nickname 또는 profileImage 중 하나는 제공되어야 함)
			if ((nickname == null || nickname.isEmpty()) &&
				(profileImage == null || profileImage.isEmpty())) {
				return UserApiResponse.badRequest("profile_update_data_required");
			}

			User updatedUser = userService.updateUserInfo(user.getId(), nickname, profileImage);
			session.setAttribute("user", updatedUser); // 세션 업데이트

			Map<String, Object> responseData = new HashMap<>();
			responseData.put("userId", idEncryptionUtil.encrypt(updatedUser.getId()));
			responseData.put("nickname", updatedUser.getNickname());
			responseData.put("profileImage", updatedUser.getProfileImage());

			return UserApiResponse.success(UserApiMessages.USER_UPDATED, responseData);
		} catch (RuntimeException e) {
			logger.warn("사용자 정보 업데이트 실패: {}", e.getMessage());
			return UserApiResponse.badRequest(e.getMessage());
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
					if (cookie.getName().equals("SESSIONID")) {
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
			return UserApiResponse.unauthorized(UserApiMessages.UNAUTHORIZED);
		}

		try {
			// 새 비밀번호와 확인 비밀번호 일치 확인
			if (!requestDTO.getNewPassword().equals(requestDTO.getConfirmPassword())) {
				return UserApiResponse.badRequest("password_mismatch");
			}

			User updatedUser = userService.changePassword(
				user.getId(),
				requestDTO.getCurrentPassword(),
				requestDTO.getNewPassword()
			);

			session.setAttribute("user", updatedUser); // 세션 업데이트

			return UserApiResponse.success(UserApiMessages.PASSWORD_CHANGED, null);
		} catch (RuntimeException e) {
			logger.warn("비밀번호 변경 실패: {}", e.getMessage());
			return UserApiResponse.badRequest(e.getMessage());
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return UserApiResponse.internalError(UserApiMessages.INTERNAL_ERROR);
		}
	}

	// 공통 세션 및 쿠키 설정 메서드
	private void setSessionAndCookie(User user, HttpSession session, HttpServletResponse response) {
		// 세션에 사용자 정보 저장
		session.setAttribute("user", user);
		session.setMaxInactiveInterval(3600); // 세션 유효시간 1시간

		// 쿠키 설정 (세션 ID 저장)
		Cookie sessionCookie = new Cookie("SESSIONID", session.getId());
		sessionCookie.setPath("/");
		sessionCookie.setHttpOnly(true);
		sessionCookie.setSecure(true); // HTTPS 환경에서만 사용
		sessionCookie.setMaxAge(3600); // 1시간 유효
		response.addCookie(sessionCookie);
	}
}
