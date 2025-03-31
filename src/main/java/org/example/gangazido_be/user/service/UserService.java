package org.example.gangazido_be.user.service;

import org.example.gangazido_be.pet.entity.Pet;
import org.example.gangazido_be.pet.repository.PetRepository;
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
import java.util.Optional;

@Service
public class UserService {
	private final UserRepository userRepository;
	private final PetRepository petRepository;
	private final Argon2PasswordEncoder passwordEncoder;
	private final UserS3FileService userS3FileService;
	private final Logger logger = LoggerFactory.getLogger(UserService.class);

	@Autowired
	public UserService(UserRepository userRepository,
		PetRepository petRepository,
		Argon2PasswordEncoder passwordEncoder,
		UserS3FileService userS3FileService) {
		this.userRepository = userRepository;
		this.petRepository = petRepository;
		this.passwordEncoder = passwordEncoder;
		this.userS3FileService = userS3FileService;
	}

	/**
	 * S3 파일 키로부터 프로필 이미지 URL을 생성하는 메서드
	 *
	 * @param fileKey S3 객체 키
	 * @return S3 URL
	 */
	public String getProfileImageUrlFromKey(String fileKey) {
		// fileKey 유효성 검사
		if (fileKey == null || fileKey.isEmpty()) {
			throw new UserValidationException("invalid_file_key", "파일 키가 유효하지 않습니다");
		}

		// S3 URL 생성 후 반환
		return userS3FileService.getS3Url(fileKey);
	}

	/**
	 * S3에 이미지가 실제로 존재하는지 확인하는 메서드
	 *
	 * @param fileKey S3 객체 키
	 * @return 존재 여부
	 */
	public boolean checkImageExists(String fileKey) {
		if (fileKey == null || fileKey.isEmpty()) {
			return false;
		}

		return userS3FileService.doesObjectExist(fileKey);
	}

	/**
	 * 프로필 이미지를 삭제하는 메서드
	 *
	 * @param profileImageUrl 프로필 이미지 URL 또는 S3 키
	 * @return 삭제 성공 여부
	 */
	public boolean deleteProfileImage(String profileImageUrl) {
		if (profileImageUrl == null || profileImageUrl.isEmpty()) {
			return false;
		}

		try {
			return userS3FileService.deleteFile(profileImageUrl);
		} catch (Exception e) {
			logger.error("프로필 이미지 삭제 실패: {}", e.getMessage());
			return false;
		}
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

		// 프로필 이미지 URL이 제공된 경우 사용
		String profileImage = userDTO.getProfileImageUrl();

		// 이미지가 없고 파일이 있는 경우 (레거시 지원)
		if ((profileImage == null || profileImage.isEmpty()) && userDTO.getProfileImage() != null && !userDTO.getProfileImage().isEmpty()) {
			logger.warn("레거시 MultipartFile 방식으로 이미지 업로드 시도 - 권장하지 않음");
			// 레거시 방식 지원 코드는 생략
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
	public User updateUserInfo(Integer userId, String nickname) {
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

		return userRepository.save(user);
	}

	// 프로필 이미지만 업데이트하는 메서드
	@Transactional
	public User updateProfileImage(Integer userId, String profileImageUrl) {
		User user = userRepository.findByIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new RuntimeException("missing_user"));

		// 기존 이미지 정보 저장
		String oldProfileImage = user.getProfileImage();

		// 새 이미지 URL 설정
		user.setProfileImage(profileImageUrl);
		User updatedUser = userRepository.save(user);

		// 저장 성공 후 기존 이미지 삭제 시도 (있는 경우에만)
		if (oldProfileImage != null && !oldProfileImage.isEmpty()) {
			try {
				userS3FileService.deleteFile(oldProfileImage);
			} catch (Exception e) {
				logger.warn("기존 프로필 이미지 삭제 실패: {}", e.getMessage());
				// 새 이미지 저장은 이미 성공했으므로 예외를 던지지 않음
			}
		}

		return updatedUser;
	}

	@Transactional
	public void deleteUser(Integer userId) {
		// userId 유효성 검사
		if (userId == null) {
			throw UserValidationException.requiredUserId();
		}

		User user = userRepository.findById(userId)
			.orElseThrow(UserAuthenticationException::missingUser);

		// 프로필 이미지 삭제 - S3에서 삭제
		if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
			userS3FileService.deleteFile(user.getProfileImage());
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
			throw new UserValidationException("required_current_password", "required_current_password");
		}

		// 새 비밀번호 유효성 검사
		if (newPassword == null || newPassword.isEmpty()) {
			throw new UserValidationException("required_new_password", "required_new_password");
		}

		// 새 비밀번호 길이 검사
		if (newPassword.length() < 8 || newPassword.length() > 20) {
			throw UserValidationException.invalidPasswordLength();
		}

		// 비밀번호 확인 검증
		if (confirmPassword == null || confirmPassword.isEmpty()) {
			throw new UserValidationException("required_new_password_confirm", "required_new_password_confirm");
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
