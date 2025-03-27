package org.example.gangazido_be.user.controller;

import org.example.gangazido_be.user.dto.UserApiResponse;
import org.example.gangazido_be.user.dto.UserLoginRequestDTO;
import org.example.gangazido_be.user.dto.UserPasswordChangeRequestDTO;
import org.example.gangazido_be.user.dto.UserDTO;
import org.example.gangazido_be.user.entity.User;
import org.example.gangazido_be.user.exception.*;
import org.example.gangazido_be.user.service.UserService;
import org.example.gangazido_be.user.util.UserApiMessages;
import org.example.gangazido_be.user.util.UserIdEncryptionUtil;
import org.example.gangazido_be.user.validator.UserPasswordValidator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
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
		@RequestPart(value = "user_email", required = false) String email,
		@RequestPart(value = "user_password", required = false) String password,
		@RequestPart(value = "user_password_confirm", required = false) String passwordConfirm,
		@RequestPart(value = "user_nickname", required = false) String nickname,
		@RequestPart(value = "user_profileImage", required = false) MultipartFile profileImage,
		HttpSession session,
		HttpServletResponse response) {
		try {
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
		} catch (UserException e) {
			logger.warn("회원가입 실패: {}", e.getMessage());
			return UserApiResponse.badRequest(e.getErrorCode());
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return UserApiResponse.internalError(UserApiMessages.INTERNAL_ERROR);
		}
	}

	@PostMapping("/login")
	@Operation(summary = "사용자 로그인", description = "이메일과 비밀번호로 로그인합니다")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		description = "로그인 정보",
		required = true,
		content = @Content(schema = @Schema(implementation = UserLoginRequestDTO.class))
	)
	public ResponseEntity<UserApiResponse<Map<String, Object>>> login(
		@RequestBody(required = false) Map<String, Object> requestMap,
		HttpSession session,
		HttpServletResponse response) {
		try {
			// 기본 null 체크만 유지하고 서비스에 위임
			if (requestMap == null) {
				logger.warn("로그인 실패: 요청 데이터가 null");
				return UserApiResponse.badRequest("required_email_and_password");
			}

			Object emailObj = requestMap.get("user_email");
			Object passwordObj = requestMap.get("user_password");

			if (emailObj == null) {
				logger.warn("로그인 실패: 이메일 필드 누락");
				return UserApiResponse.badRequest("required_email");
			}

			if (passwordObj == null) {
				logger.warn("로그인 실패: 비밀번호 필드 누락");
				return UserApiResponse.badRequest("required_password");
			}

			String email = emailObj.toString();
			String password = passwordObj.toString();

			// 서비스 호출 - 유효성 검증은 서비스에서 처리
			User user = userService.login(email, password);

			// 세션 및 쿠키 설정
			setSessionAndCookie(user, session, response);

			Map<String, Object> responseData = new HashMap<>();
			responseData.put("nickname", user.getNickname());

			return UserApiResponse.success(UserApiMessages.LOGIN_SUCCESS, responseData);
		} catch (UserException e) {
			logger.warn("로그인 실패: {}", e.getMessage());
			return UserApiResponse.badRequest(e.getErrorCode());
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return UserApiResponse.internalError("internal_server_error");
		}
	}

	// 사용자 정보 확인 API
	@GetMapping("/me")
	public ResponseEntity<UserApiResponse<Map<String, Object>>> getCurrentUser(HttpServletRequest request, HttpSession session) {
		// 모든 쿠키 출력
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				logger.info("쿠키: {}={}", cookie.getName(), cookie.getValue());
			}
		} else {
			logger.warn("요청에 쿠키가 없습니다!");
		}

		// 세션 정보 출력
		logger.info("세션 ID: {}", session.getId());
		logger.info("사용자 정보: {}", session.getAttribute("user"));

		try {
			User user = (User) session.getAttribute("user");

			if (user == null) {
				throw UserAuthenticationException.unauthorized();
			}

			Map<String, Object> responseData = new HashMap<>();
			responseData.put("userId", idEncryptionUtil.encrypt(user.getId()));
			responseData.put("email", user.getEmail());
			responseData.put("nickname", user.getNickname());
			responseData.put("profileImage", user.getProfileImage());

			return UserApiResponse.success(UserApiMessages.SUCCESS, responseData);
		} catch (UserException e) {
			logger.warn("사용자 정보 조회 실패: {}", e.getMessage());
			return UserApiResponse.badRequest(e.getErrorCode());
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return UserApiResponse.internalError(UserApiMessages.INTERNAL_ERROR);
		}
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
					if (cookie.getName().equals("JSESSIONID")) {
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
	public ResponseEntity<UserApiResponse<Map<String, Boolean>>> checkEmailDuplicate(
		@RequestParam(required = false) String email) {
		try {
			boolean isDuplicate = userService.isEmailDuplicate(email);
			Map<String, Boolean> responseData = Map.of("isDuplicate", isDuplicate);
			return UserApiResponse.success(UserApiMessages.SUCCESS, responseData);
		} catch (UserException e) {
			logger.warn("이메일 중복 체크 실패: {}", e.getMessage());
			return UserApiResponse.badRequest(e.getErrorCode());
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return UserApiResponse.internalError(UserApiMessages.INTERNAL_ERROR);
		}
	}

	// 중복 닉네임 확인 API
	@GetMapping("/check-nickname")
	public ResponseEntity<UserApiResponse<Map<String, Boolean>>> checkNicknameDuplicate(
		@RequestParam(required = false) String nickname) {
		try {
			boolean isDuplicate = userService.isNicknameDuplicate(nickname);
			logger.info("닉네임 중복 체크: {}, 결과: {}", nickname, isDuplicate);
			Map<String, Boolean> responseData = Map.of("isDuplicate", isDuplicate);
			return UserApiResponse.success(UserApiMessages.SUCCESS, responseData);
		} catch (UserException e) {
			logger.warn("닉네임 중복 체크 실패: {}", e.getMessage());
			return UserApiResponse.badRequest(e.getErrorCode());
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return UserApiResponse.internalError(UserApiMessages.INTERNAL_ERROR);
		}
	}

	@PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<UserApiResponse<Map<String, Object>>> updateMyInfo(
		@RequestPart(value = "user_nickname", required = false) String nickname,
		@RequestPart(value = "user_profile_image", required = false) MultipartFile profileImage,
		HttpSession session) {
		try {
			User user = (User) session.getAttribute("user");
			if (user == null) {
				throw UserAuthenticationException.unauthorized();
			}

			// 기본 입력 확인만 수행하고 서비스로 위임
			if ((nickname == null || nickname.isEmpty()) && (profileImage == null || profileImage.isEmpty())) {
				throw new UserValidationException("required_profile_update_data", "required_profile_update_data");
			}

			// 서비스 호출 - 모든 유효성 검증은 서비스에서 처리
			User updatedUser = userService.updateUserInfo(user.getId(), nickname, profileImage);
			session.setAttribute("user", updatedUser); // 세션 업데이트

			Map<String, Object> responseData = new HashMap<>();
			responseData.put("userId", idEncryptionUtil.encrypt(updatedUser.getId()));
			responseData.put("nickname", updatedUser.getNickname());
			responseData.put("profileImage", updatedUser.getProfileImage());

			return UserApiResponse.success("update_user_data_success", responseData);
		} catch (UserException e) {
			logger.warn("사용자 정보 업데이트 실패: {}", e.getMessage());
			return UserApiResponse.badRequest(e.getErrorCode());
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
		try {
			User user = (User) session.getAttribute("user");
			if (user == null) {
				throw UserAuthenticationException.unauthorized();
			}

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
		} catch (UserException e) {
			logger.warn("회원 탈퇴 실패: {}", e.getMessage());
			return UserApiResponse.badRequest(e.getErrorCode());
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return UserApiResponse.internalError(UserApiMessages.INTERNAL_ERROR);
		}
	}

	@PatchMapping("/me/password")
	public ResponseEntity<UserApiResponse<Object>> changePassword(
		@Valid @RequestBody(required = false) UserPasswordChangeRequestDTO requestDTO,
		HttpSession session) {
		try {
			// 기본 null 체크만 수행
			if (requestDTO == null) {
				throw new UserValidationException("required_password_data", "required_password_data");
			}

			User user = (User) session.getAttribute("user");
			if (user == null) {
				throw UserAuthenticationException.unauthorized();
			}

			// 서비스 호출 - 유효성 검증은 서비스에서 처리
			// 단, 서비스 메소드를 수정하여 비밀번호 확인까지 처리하도록 변경 필요
			User updatedUser = userService.changePassword(
				user.getId(),
				requestDTO.getCurrentPassword(),
				requestDTO.getNewPassword(),
				requestDTO.getConfirmPassword() // 서비스 메소드 수정 필요
			);

			session.setAttribute("user", updatedUser); // 세션 업데이트

			return UserApiResponse.success(UserApiMessages.PASSWORD_CHANGED, null);
		} catch (UserException e) {
			logger.warn("비밀번호 변경 실패: {}", e.getMessage());
			return UserApiResponse.badRequest(e.getErrorCode());
		} catch (Exception e) {
			logger.error("서버 오류: ", e);
			return UserApiResponse.internalError("internal_server_error");
		}
	}

	// 공통 세션 및 쿠키 설정 메서드
	private void setSessionAndCookie(User user, HttpSession session, HttpServletResponse response) {
		// // 세션에 사용자 정보 저장
		// session.setAttribute("user", user);
		// session.setMaxInactiveInterval(3600); // 세션 유효시간 1시간
		//
		// // 쿠키 설정 (세션 ID 저장)
		// Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
		// sessionCookie.setPath("/");
		// sessionCookie.setHttpOnly(true);
		// sessionCookie.setSecure(false); // HTTPS 환경에서만 사용 해제
		// sessionCookie.setMaxAge(3600); // 1시간 유효
		// response.addCookie(sessionCookie);
		//
		// // 디버깅 로그 추가
		// logger.debug("쿠키 설정 완료: {}, 세션 ID: {}", sessionCookie.getName(), session.getId());

		// 세션에 사용자 정보 저장
		session.setAttribute("user", user);
		session.setMaxInactiveInterval(3600); // 세션 유효시간 1시간

		// 쿠키 설정 (세션 ID 저장)
		Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());

		// 모든 경로에서 접근 가능하도록 설정
		sessionCookie.setPath("/");

		// XSS 공격 방지를 위한 HttpOnly 활성화
		sessionCookie.setHttpOnly(true);

		// HTTPS 환경이므로 Secure 활성화
		sessionCookie.setSecure(true);

		// 도메인 설정 (www 서브도메인 포함)
		sessionCookie.setDomain("gangazido.com");

		// 1시간 유효
		sessionCookie.setMaxAge(3600);

		// SameSite 설정 (브라우저 호환성을 위해 자바 쿠키에서는 직접 헤더 설정 필요)
		// 현재 자바의 Cookie 클래스는 SameSite 속성을 직접 지원하지 않음
		response.setHeader("Set-Cookie", sessionCookie.getName() + "=" + sessionCookie.getValue() +
			"; Max-Age=" + sessionCookie.getMaxAge() +
			"; Path=" + sessionCookie.getPath() +
			"; Domain=" + sessionCookie.getDomain() +
			"; HttpOnly" +
			"; Secure" +
			"; SameSite=None");

		// 디버깅 로그 추가
		logger.debug("쿠키 설정 완료: {}, 세션 ID: {}, 도메인: {}",
			sessionCookie.getName(), session.getId(), sessionCookie.getDomain());
	}
}
