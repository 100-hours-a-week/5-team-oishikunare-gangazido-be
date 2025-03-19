package org.example.gangazido_be.user.service;

import org.example.gangazido_be.user.dto.UserDTO;
import org.example.gangazido_be.user.entity.User;
import org.example.gangazido_be.user.repository.UserRepository;
import org.example.gangazido_be.user.validator.PasswordValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

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
		// 이메일 중복 체크
		if (isEmailDuplicate(userDTO.getEmail())) {
			throw new RuntimeException("duplicate_email");
		}

		// 닉네임 중복 체크
		if (isNicknameDuplicate(userDTO.getNickname())) {
			throw new RuntimeException("duplicate_nickname");
		}

		// 비밀번호 복잡성 검증
		if (!PasswordValidator.isValid(userDTO.getPassword())) {
			throw new RuntimeException(PasswordValidator.getValidationMessage());
		}

		// 프로필 이미지 처리
		String profileImage = null;
		if (userDTO.getProfileImage() != null && !userDTO.getProfileImage().isEmpty()) {
			profileImage = userFileService.saveProfileImage(userDTO.getProfileImage());
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
		// 이메일로 사용자 조회
		User user = userRepository.findByEmailAndDeletedAtIsNull(email)
			.orElseThrow(() -> {
				logger.warn("로그인 실패: 이메일 {} 에 해당하는 사용자가 없습니다.", email);
				return new RuntimeException("missing_email");
			});

		// 입력된 패스워드 로깅 (실제 환경에서는 보안상 위험하므로 개발 환경에서만 사용)
		logger.debug("로그인 시도: 이메일={}, 패스워드 길이={}", email, password.length());

		// 비밀번호 검증
		boolean matches = passwordEncoder.matches(password, user.getPassword());
		if (!matches) {
			logger.warn("로그인 실패: 이메일 {} 사용자의 비밀번호가 일치하지 않습니다.", email);
			throw new RuntimeException("invalid_password");
		}

		return user;
	}

	// 중복 이메일 확인
	public boolean isEmailDuplicate(String email) {
		return userRepository.findByEmailAndDeletedAtIsNull(email).isPresent();
	}

	// 중복 닉네임 확인
	public boolean isNicknameDuplicate(String nickname) {
		return userRepository.findByNicknameAndDeletedAtIsNull(nickname).isPresent();
	}

	// 프로필 이미지 업데이트
	@Transactional
	public User updateProfileImage(Integer userId, MultipartFile newProfileImage) {
		User user = userRepository.findByIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new RuntimeException("missing_user"));

		// 기존 이미지가 있으면 삭제
		if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
			userFileService.deleteImage(user.getProfileImage());
		}

		// 새 이미지 저장
		String newImage = userFileService.saveProfileImage(newProfileImage);
		user.setProfileImage(newImage);

		return userRepository.save(user);
	}

	@Transactional
	public User updateUserInfo(Integer userId, String nickname, MultipartFile profileImage) {
		User user = userRepository.findByIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new RuntimeException("missing_user"));

		// 닉네임 업데이트 (변경 시에만)
		if (nickname != null && !nickname.isEmpty() && !nickname.equals(user.getNickname())) {
			// 제어 문자 제거
			nickname = nickname.replaceAll("[\\p{Cntrl}]", "");

			// 닉네임 유효성 검사 (선택적)
			if (nickname.isEmpty()) {
				throw new RuntimeException("invalid_nickname_format");
			}

			// 닉네임 중복 체크
			if (isNicknameDuplicate(nickname)) {
				throw new RuntimeException("duplicate_nickname");
			}
			user.setNickname(nickname);
		}

		// 프로필 이미지 업데이트 (첨부된 경우에만)
		if (profileImage != null && !profileImage.isEmpty()) {
			// 기존 이미지가 있으면 삭제
			if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
				userFileService.deleteImage(user.getProfileImage());
			}

			// 새 이미지 저장
			String newImage = userFileService.saveProfileImage(profileImage);
			user.setProfileImage(newImage);
		}

		return userRepository.save(user);
	}

	@Transactional
	public void deleteUser(Integer userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new RuntimeException("missing_user"));

		// 프로필 이미지 삭제
		if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
			userFileService.deleteImage(user.getProfileImage());
		}

		// 논리적 삭제 (deletedAt 설정)
		user.setDeletedAt(LocalDateTime.now());
		userRepository.save(user);
	}

	@Transactional
	public User changePassword(Integer userId, String currentPassword, String newPassword) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new RuntimeException("missing_user"));

		// 현재 비밀번호 확인
		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
			throw new RuntimeException("invalid_current_password");
		}

		// 비밀번호 복잡성 검증
		if (!PasswordValidator.isValid(newPassword)) {
			throw new RuntimeException(PasswordValidator.getValidationMessage());
		}

		// 새 비밀번호 설정
		user.setPassword(passwordEncoder.encode(newPassword));

		return userRepository.save(user);
	}
}
