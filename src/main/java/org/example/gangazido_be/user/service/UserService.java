package org.example.gangazido_be.user.service;

import org.example.gangazido_be.user.dto.UserDTO;
import org.example.gangazido_be.user.entity.User;
import org.example.gangazido_be.user.exception.*;
import org.example.gangazido_be.user.repository.UserRepository;
import org.example.gangazido_be.user.validator.UserPasswordValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class UserService {
	private final UserRepository userRepository;
	private final Argon2PasswordEncoder passwordEncoder;
	private final UserFileService userFileService;
	private final Logger logger = LoggerFactory.getLogger(UserService.class);

	@Autowired
	public UserService(UserRepository userRepository,
		Argon2PasswordEncoder passwordEncoder,
		UserFileService userFileService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.userFileService = userFileService;
	}

	@Transactional
	public User registerUser(UserDTO userDTO) {
		// 이메일 형식 유효성 검사
		if (userDTO.getEmail() == null || userDTO.getEmail().isEmpty()) {
			throw UserValidationException.requiredEmail();
		}

		if (!userDTO.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
			throw UserValidationException.invalidEmailFormat();
		}

		// 비밀번호 유효성 검사
		if (userDTO.getPassword() == null || userDTO.getPassword().isEmpty()) {
			throw UserValidationException.requiredPassword();
		}

		// 비밀번호 길이 검사
		if (userDTO.getPassword().length() < 8 || userDTO.getPassword().length() > 20) {
			throw UserValidationException.invalidPasswordLength();
		}

		// 비밀번호 복잡성 검증
		if (!UserPasswordValidator.isValid(userDTO.getPassword())) {
			throw UserValidationException.invalidPasswordFormat();
		}

		// 닉네임 유효성 검사
		if (userDTO.getNickname() == null || userDTO.getNickname().isEmpty()) {
			throw UserValidationException.requiredNickname();
		}

		// 닉네임 길이 검사
		if (userDTO.getNickname().length() < 2 || userDTO.getNickname().length() > 20) {
			throw UserValidationException.invalidNicknameLength();
		}

		// 이메일 중복 체크
		if (isEmailDuplicate(userDTO.getEmail())) {
			throw UserDuplicateException.duplicateEmail();
		}

		// 닉네임 중복 체크
		if (isNicknameDuplicate(userDTO.getNickname())) {
			throw UserDuplicateException.duplicateNickname();
		}

		// 프로필 이미지 처리
		String profileImage = null;
		if (userDTO.getProfileImage() != null && !userDTO.getProfileImage().isEmpty()) {
			try {
				// 이미지 크기 검사
				if (userDTO.getProfileImage().getSize() > 5 * 1024 * 1024) {
					throw UserFileException.fileTooLarge();
				}

				// 이미지 타입 검사
				String contentType = userDTO.getProfileImage().getContentType();
				if (contentType == null || !contentType.startsWith("image/")) {
					throw UserFileException.invalidFileType();
				}

				profileImage = userFileService.saveProfileImage(userDTO.getProfileImage());
			} catch (RuntimeException e) {
				throw UserFileException.uploadError(e.getMessage());
			}
		}

		User newUser = User.builder()
			.email(userDTO.getEmail())
			.password(passwordEncoder.encode(userDTO.getPassword()))
			.nickname(userDTO.getNickname())
			.profileImage(profileImage)
			.build();

		return userRepository.save(newUser);
	}

	public User login(String email, String password) {
		// 이메일 유효성 검사
		if (email == null || email.isEmpty()) {
			throw UserValidationException.requiredEmail();
		}

		// 이메일 형식 검증
		if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
			throw UserValidationException.invalidEmailFormat();
		}

		// 비밀번호 유효성 검사
		if (password == null || password.isEmpty()) {
			throw UserValidationException.requiredPassword();
		}

		// 이메일로 사용자 조회
		User user = userRepository.findByEmailAndDeletedAtIsNull(email)
			.orElseThrow(() -> {
				logger.warn("로그인 실패: 이메일 {} 에 해당하는 사용자가 없습니다.", email);
				return UserAuthenticationException.invalidEmail();
			});

		// 비밀번호 검증
		boolean matches = passwordEncoder.matches(password, user.getPassword());
		if (!matches) {
			logger.warn("로그인 실패: 이메일 {} 사용자의 비밀번호가 일치하지 않습니다.", email);
			throw UserAuthenticationException.invalidPassword();
		}

		return user;
	}

	// 중복 이메일 확인
	public boolean isEmailDuplicate(String email) {
		if (email == null || email.isEmpty()) {
			throw UserValidationException.requiredEmail();
		}

		return userRepository.findByEmailAndDeletedAtIsNull(email).isPresent();
	}

	// 중복 닉네임 확인
	public boolean isNicknameDuplicate(String nickname) {
		if (nickname == null || nickname.isEmpty()) {
			throw UserValidationException.requiredNickname();
		}

		return userRepository.findByNicknameAndDeletedAtIsNull(nickname).isPresent();
	}

	@Transactional
	public User updateUserInfo(Integer userId, String nickname, MultipartFile profileImage) {
		// userId 유효성 검사
		if (userId == null) {
			throw UserValidationException.requiredUserId();
		}

		User user = userRepository.findByIdAndDeletedAtIsNull(userId)
			.orElseThrow(UserAuthenticationException::missingUser);

		// 닉네임 업데이트 (변경 시에만)
		if (nickname != null && !nickname.isEmpty() && !nickname.equals(user.getNickname())) {
			// 제어 문자 제거
			nickname = nickname.replaceAll("[\\p{Cntrl}]", "");

			// 닉네임 유효성 검사
			if (nickname.isEmpty()) {
				throw UserValidationException.requiredNickname();
			}

			// 길이 제한 검사
			if (nickname.length() < 2 || nickname.length() > 20) {
				throw UserValidationException.invalidNicknameLength();
			}

			// 닉네임 중복 체크
			if (isNicknameDuplicate(nickname)) {
				throw UserDuplicateException.duplicateNickname();
			}
			user.setNickname(nickname);
		}

		// 프로필 이미지 업데이트 (첨부된 경우에만)
		if (profileImage != null && !profileImage.isEmpty()) {
			// 파일 크기 검사 (예: 5MB 제한)
			if (profileImage.getSize() > 5 * 1024 * 1024) {
				throw UserFileException.fileTooLarge();
			}

			// 파일 형식 검사 (이미지 파일만 허용)
			String contentType = profileImage.getContentType();
			if (contentType == null || !contentType.startsWith("image/")) {
				throw UserFileException.invalidFileType();
			}

			try {
				// 기존 이미지가 있으면 삭제
				if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
					userFileService.deleteImage(user.getProfileImage());
				}

				// 새 이미지 저장
				String newImage = userFileService.saveProfileImage(profileImage);
				user.setProfileImage(newImage);
			} catch (RuntimeException e) {
				throw UserFileException.uploadError(e.getMessage());
			}
		}

		return userRepository.save(user);
	}

	@Transactional
	public void deleteUser(Integer userId) {
		// userId 유효성 검사
		if (userId == null) {
			throw UserValidationException.requiredUserId();
		}

		User user = userRepository.findById(userId)
			.orElseThrow(UserAuthenticationException::missingUser);

		// 프로필 이미지 삭제
		if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
			try {
				userFileService.deleteImage(user.getProfileImage());
			} catch (RuntimeException e) {
				logger.warn("이미지 삭제 실패: {}", e.getMessage());
				// 이미지 삭제 실패는 사용자 삭제를 중단시키지 않음
			}
		}

		// 논리적 삭제 (deletedAt 설정)
		user.setDeletedAt(LocalDateTime.now());
		userRepository.save(user);
	}

	@Transactional
	public User changePassword(Integer userId, String currentPassword, String newPassword, String confirmPassword) {
		// userId 유효성 검사
		if (userId == null) {
			throw UserValidationException.requiredUserId();
		}

		// 현재 비밀번호 유효성 검사
		if (currentPassword == null || currentPassword.isEmpty()) {
			throw new UserValidationException("required_current_password", "현재 비밀번호를 입력해주세요.");
		}

		// 새 비밀번호 유효성 검사
		if (newPassword == null || newPassword.isEmpty()) {
			throw new UserValidationException("required_new_password", "새 비밀번호를 입력해주세요.");
		}

		// 새 비밀번호 길이 검사
		if (newPassword.length() < 8 || newPassword.length() > 20) {
			throw UserValidationException.invalidPasswordLength();
		}

		// 비밀번호 확인 검증
		if (confirmPassword == null || confirmPassword.isEmpty()) {
			throw new UserValidationException("required_new_password_confirm", "새 비밀번호 확인을 입력해주세요.");
		}

		// 비밀번호 일치 검증
		if (!newPassword.equals(confirmPassword)) {
			throw UserValidationException.passwordsDoNotMatch();
		}

		User user = userRepository.findById(userId)
			.orElseThrow(UserAuthenticationException::missingUser);

		// 현재 비밀번호 확인
		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
			throw UserAuthenticationException.invalidCurrentPassword();
		}

		// 비밀번호 복잡성 검증
		if (!UserPasswordValidator.isValid(newPassword)) {
			throw UserValidationException.invalidPasswordFormat();
		}

		// 새 비밀번호 설정
		user.setPassword(passwordEncoder.encode(newPassword));

		return userRepository.save(user);
	}
}
