package org.example.gangazido_be.user.controller;

import org.example.gangazido_be.user.dto.LoginRequestDTO;
import org.example.gangazido_be.user.dto.PasswordChangeRequestDTO;
import org.example.gangazido_be.user.dto.UserDTO;
import org.example.gangazido_be.user.entity.User;
import org.example.gangazido_be.user.service.UserService;
import org.example.gangazido_be.user.util.IdEncryptionUtil;
import org.example.gangazido_be.user.validator.PasswordValidator;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
	private final IdEncryptionUtil idEncryptionUtil;
	private final Logger logger = LoggerFactory.getLogger(UserController.class);

	@Autowired
	public UserController(UserService userService, IdEncryptionUtil idEncryptionUtil) {
		this.userService = userService;
		this.idEncryptionUtil = idEncryptionUtil;
	}

	@PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Map<String, Object>> registerUser(
		@RequestPart("user_email") String email,
		@RequestPart("user_password") String password,
		@RequestPart("user_nickname") String nickname,
		@RequestPart(value = "user_profileImage", required = false) MultipartFile profileImage,
		HttpSession session,
		HttpServletResponse response) {
		try {
			// 입력값 검증
			if (email == null || email.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "이메일은 필수 입력 항목입니다."));
			}

			if (password == null || password.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "비밀번호는 필수 입력 항목입니다."));
			}

			if (nickname == null || nickname.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "닉네임은 필수 입력 항목입니다."));
			}

			// 비밀번호 복잡성 검증
			if (!PasswordValidator.isValid(password)) {
				return ResponseEntity.badRequest().body(Map.of("error", PasswordValidator.getValidationMessage()));
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

			Map<String, Object> responseBody = new HashMap<>();
			responseBody.put("message", "회원가입 성공");
			responseBody.put("userId", encryptedId);
			responseBody.put("nickname", registeredUser.getNickname());

			return ResponseEntity.ok(responseBody);
		} catch (RuntimeException e) {
			logger.warn("회원가입 실패: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "서버 오류가 발생했습니다."));
		}
	}

	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequestDTO loginRequest,
		HttpSession session,
		HttpServletResponse response) {
		try {
			User user = userService.login(loginRequest.getEmail(), loginRequest.getPassword());

			// 세션 및 쿠키 설정
			setSessionAndCookie(user, session, response);

			// 사용자 ID를 암호화하여 응답
			String encryptedId = idEncryptionUtil.encrypt(user.getId());

			Map<String, Object> responseBody = new HashMap<>();
			responseBody.put("message", "로그인 성공");
			responseBody.put("userId", encryptedId);
			responseBody.put("nickname", user.getNickname());

			return ResponseEntity.ok(responseBody);
		} catch (RuntimeException e) {
			logger.warn("로그인 실패: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "서버 오류가 발생했습니다."));
		}
	}

	// 로그인 상태 확인 API
	@GetMapping("/me")
	public ResponseEntity<Map<String, Object>> getCurrentUser(HttpSession session) {
		User user = (User) session.getAttribute("user");

		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(Map.of("error", "로그인이 필요합니다."));
		}

		Map<String, Object> responseBody = new HashMap<>();
		responseBody.put("userId", idEncryptionUtil.encrypt(user.getId()));
		responseBody.put("email", user.getEmail());
		responseBody.put("nickname", user.getNickname());
		responseBody.put("profileImage", user.getProfileImage());

		return ResponseEntity.ok(responseBody);
	}

	@PostMapping("/logout")
	public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request,
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

			return ResponseEntity.ok(Map.of("message", "로그아웃 성공"));
		} catch (Exception e) {
			logger.error("로그아웃 처리 중 오류: ", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "로그아웃 처리 중 오류가 발생했습니다."));
		}
	}

	// 중복 이메일 확인 API
	@GetMapping("/check-email")
	public ResponseEntity<Map<String, Object>> checkEmailDuplicate(@RequestParam String email) {
		boolean isDuplicate = userService.isEmailDuplicate(email);
		return ResponseEntity.ok(Map.of("isDuplicate", isDuplicate));
	}

	// 중복 닉네임 확인 API
	@GetMapping("/check-nickname")
	public ResponseEntity<Map<String, Object>> checkNicknameDuplicate(@RequestParam String nickname) {
		boolean isDuplicate = userService.isNicknameDuplicate(nickname);
		return ResponseEntity.ok(Map.of("isDuplicate", isDuplicate));
	}

	// 프로필 이미지 업데이트 API
	@PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Map<String, Object>> updateProfileImage(
		@RequestPart("profileImage") MultipartFile profileImage,
		HttpSession session) {

		User user = (User) session.getAttribute("user");
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(Map.of("error", "로그인이 필요합니다."));
		}

		try {
			User updatedUser = userService.updateProfileImage(user.getId(), profileImage);
			session.setAttribute("user", updatedUser); // 세션 업데이트

			return ResponseEntity.ok(Map.of(
				"message", "프로필 이미지가 업데이트되었습니다.",
				"profileImage", updatedUser.getProfileImage()
			));
		} catch (Exception e) {
			logger.error("프로필 이미지 업데이트 중 오류: ", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "프로필 이미지 업데이트 중 오류가 발생했습니다."));
		}
	}

	@PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Map<String, Object>> updateMyInfo(
		@RequestPart(value = "user_nickname", required = false) String nickname,
		@RequestPart(value = "user_profile_image", required = false) MultipartFile profileImage,
		HttpSession session) {

		User user = (User) session.getAttribute("user");
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(Map.of("error", "로그인이 필요합니다."));
		}

		try {
			// 닉네임 정제 - 제어 문자 제거
			if (nickname != null && !nickname.isEmpty()) {
				nickname = nickname.replaceAll("[\\p{Cntrl}]", "");

				// 빈 문자열이 되면 처리
				if (nickname.isEmpty()) {
					return ResponseEntity.badRequest()
						.body(Map.of("error", "유효한 닉네임이 필요합니다."));
				}
			}

			// 정보 업데이트 (nickname 또는 profileImage 중 하나는 제공되어야 함)
			if ((nickname == null || nickname.isEmpty()) &&
				(profileImage == null || profileImage.isEmpty())) {
				return ResponseEntity.badRequest()
					.body(Map.of("error", "닉네임 또는 프로필 이미지가 필요합니다."));
			}

			User updatedUser = userService.updateUserInfo(user.getId(), nickname, profileImage);
			session.setAttribute("user", updatedUser); // 세션 업데이트

			Map<String, Object> responseBody = new HashMap<>();
			responseBody.put("message", "사용자 정보가 성공적으로 업데이트되었습니다.");
			responseBody.put("userId", idEncryptionUtil.encrypt(updatedUser.getId()));
			responseBody.put("nickname", updatedUser.getNickname());
			responseBody.put("profileImage", updatedUser.getProfileImage());

			return ResponseEntity.ok(responseBody);
		} catch (RuntimeException e) {
			logger.warn("사용자 정보 업데이트 실패: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "서버 오류가 발생했습니다."));
		}
	}

	@DeleteMapping("/me")
	public ResponseEntity<Map<String, Object>> deleteMyAccount(
		HttpServletRequest request,
		HttpServletResponse response,
		HttpSession session) {

		User user = (User) session.getAttribute("user");
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(Map.of("error", "로그인이 필요합니다."));
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

			return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 완료되었습니다."));
		} catch (RuntimeException e) {
			logger.warn("회원 탈퇴 실패: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "서버 오류가 발생했습니다."));
		}
	}

	@PatchMapping("/me/password")
	public ResponseEntity<Map<String, Object>> changePassword(
		@Valid @RequestBody PasswordChangeRequestDTO requestDTO,
		HttpSession session) {

		User user = (User) session.getAttribute("user");
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(Map.of("error", "로그인이 필요합니다."));
		}

		try {
			// 새 비밀번호와 확인 비밀번호 일치 확인
			if (!requestDTO.getNewPassword().equals(requestDTO.getConfirmPassword())) {
				return ResponseEntity.badRequest()
					.body(Map.of("error", "새 비밀번호와 확인 비밀번호가 일치하지 않습니다."));
			}

			User updatedUser = userService.changePassword(
				user.getId(),
				requestDTO.getCurrentPassword(),
				requestDTO.getNewPassword()
			);

			session.setAttribute("user", updatedUser); // 세션 업데이트

			return ResponseEntity.ok(Map.of("message", "비밀번호가 성공적으로 변경되었습니다."));
		} catch (RuntimeException e) {
			logger.warn("비밀번호 변경 실패: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "서버 오류가 발생했습니다."));
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
