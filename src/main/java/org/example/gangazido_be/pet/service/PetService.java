package org.example.gangazido_be.pet.service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.gangazido_be.pet.dto.PetResponse;
import org.example.gangazido_be.pet.entity.Pet;
import org.example.gangazido_be.pet.exception.PetException;
import org.example.gangazido_be.pet.exception.PetExceptionType;
import org.example.gangazido_be.pet.exception.PetNotFoundException;
import org.example.gangazido_be.pet.exception.UserNotFoundException;
import org.example.gangazido_be.pet.repository.PetRepository;
import org.example.gangazido_be.user.entity.User;
import org.example.gangazido_be.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetService {
	private final PetRepository petRepository;
	private final UserRepository userRepository;

	@Value("${app.pet.upload.dir}")
	private final String uploadDir;

	// 서버 시작 시 업로드 디렉토리 생성 확인
	@PostConstruct
	public void initUploadDirectory() {
		File uploadPath = new File(uploadDir);
		if (!uploadPath.exists() && !uploadPath.mkdirs()) {
			throw new RuntimeException("❌ 디렉토리 생성 실패: " + uploadDir);
		}
		System.out.println("✅ 파일 업로드 디렉토리 초기화 완료: " + uploadDir);
	}

	// 반려견 정보 등록
	@Transactional
	public PetResponse createPet(Integer userId, String name, Integer age, Boolean gender, String breed, Double weight, MultipartFile petProfileImage) {
		String petProfileImagePath = null;

		// 사용자 존재 여부 확인 (없으면 404  예외)
		User user = userRepository.findByIdAndDeletedAtIsNull(userId) // userId로 User 엔티티 조회
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_USER.getMessage()));

		// 이미 해당 userId로 pet이 존재한다면 예외 처리
		if (petRepository.existsByUserIdAndDeletedAtIsNull(userId)) {
			throw new PetException(HttpStatus.CONFLICT, PetExceptionType.ALREADY_EXISTS_PET.getMessage());
		}

		// soft-delete된 반려견 있는지 확인
		Pet deletedPet = petRepository.findByUserIdIncludingDeleted(userId)
			.filter(p -> p.getDeletedAt() != null)
			.orElse(null);

		if (deletedPet != null) {
			log.debug("✔️ 소프트 삭제된 반려견 복구 로직 실행됨");

			deletedPet.setDeletedAt(null);
			deletedPet.setUser(user); // 중요!!

			if (petProfileImage != null && !petProfileImage.isEmpty()) {
				petProfileImagePath = saveFile(petProfileImage);
			}

			deletedPet.updatePet(name, petProfileImagePath, age, gender, breed, weight);
			return PetResponse.from(petRepository.save(deletedPet));
		}

		// 유효성 검증
		if (name == null || name.isBlank()) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_NAME.getMessage());
		}
		if (!name.matches("^[a-zA-Z가-힣0-9 ]+$")) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_NAME_FORMAT.getMessage());
		}
		if (name.length() > 10) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_NAME_LENGTH.getMessage());
		}
		if (breed == null || breed.isBlank()) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_BREED.getMessage());
		}
		if (age <= 0 || age > 50) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_AGE_VALUE.getMessage());
		}
		if (weight < 0.1 || weight > 200.0) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_WEIGHT.getMessage());
		}
		if (gender == null) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_GENDER.getMessage());
		}
		if (breed == null || breed.isBlank()) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_BREED.getMessage());
		}
		if (age <= 0 || age > 50) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_AGE_VALUE.getMessage());
		}
		if (weight < 0.1 || weight > 200.0) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_WEIGHT.getMessage());
		}

		// ✅ 파일 저장 처리
		if (petProfileImage != null && !petProfileImage.isEmpty()) {
			petProfileImagePath = saveFile(petProfileImage);
		}

		// 엔티티 생성 및 저장
		Pet pet = Pet.builder()
			.user(user)
			.name(name)
			.petProfileImage(petProfileImagePath) // ✅ 저장된 파일 URL 설정
			.age(age)
			.gender(gender)
			.breed(breed)
			.weight(weight)
			.build();

		Pet savedPet = petRepository.save(pet);
		return PetResponse.from(savedPet);
	}

	@Transactional
	public PetResponse updatePetProfileImage(Integer userId, MultipartFile petProfileImage) {
		Pet pet = petRepository.findByUserId(userId)
			.orElseThrow(PetNotFoundException::new);

		// ✅ 기존 이미지 파일 삭제 (선택적)
		deleteOldFile(pet.getPetProfileImage());

		// ✅ 새 이미지 저장 후 URL 설정
		String petProfileImageUrl = saveFile(petProfileImage);
		pet.setPetProfileImage(petProfileImageUrl);
		petRepository.save(pet);

		return PetResponse.from(pet);
	}

	private String saveFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			return null;
		}
		String originalFileName = file.getOriginalFilename();
		if (originalFileName == null) {
			throw new RuntimeException("❌ 업로드된 파일 이름이 null입니다.");
		}

		String fileName = UUID.randomUUID() + "_" + originalFileName;
		String absoluteUploadDir = new File(uploadDir).getAbsolutePath(); // 절대 경로
		String fullPath = absoluteUploadDir + File.separator + fileName;

		File destination = new File(fullPath);
		File parentDir = destination.getParentFile();

		if (!parentDir.exists() && !parentDir.mkdirs()) {
			log.error("❌ 업로드 폴더 생성 실패: {}", parentDir.getAbsolutePath());
			throw new RuntimeException("폴더 생성 실패: " + parentDir.getAbsolutePath());
		}

		try {
			log.info("📎 업로드된 파일명: {}", originalFileName);
			log.info("📁 저장할 전체 경로: {}", fullPath);
			file.transferTo(destination);
		} catch (IOException e) {
			log.error("❌ 파일 저장 실패! 경로: {}, 에러: {}", fullPath, e.getMessage(), e);
			throw new RuntimeException("파일 저장 실패", e);
		}

		// 웹에서 접근 가능한 경로로 반환
		return "/uploads/pet/" + fileName;
	}

	// ✅ 기존 이미지 파일 삭제 메서드 (선택적)
	private void deleteOldFile(String filePath) {
		if (filePath != null && filePath.startsWith("/uploads/pet/")) {
			File oldFile = new File(filePath);
			if (oldFile.exists()) {
				boolean deleted = oldFile.delete(); // ✅ 삭제 성공 여부 확인
				if (!deleted) {
					System.err.println("파일 삭제 실패: " + oldFile.getAbsolutePath()); // ✅ 삭제 실패 시 로그 출력
				}
			}
		}
	}

	// 반려견 정보 조회
	@Transactional(readOnly = true)
	public PetResponse getPet(Integer userId) {
		// 사용자가 존재하는지 확인 (없으면 404 예외)
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_USER.getMessage()));

		// 해당 사용자의 반려견 정보 조회 (없으면 404 예외)
		Pet pet = petRepository.findByUserId(userId)
			.orElseThrow(PetNotFoundException::new);

		return PetResponse.from(pet);
	}

	// 반려견 정보 수정
	@Transactional
	public PetResponse updatePet(Integer userId,  String name, Integer age, Boolean gender, String breed, Double weight, MultipartFile petProfileImage) {
		// 사용자 존재 여부 확인 (없으면 404 예외)
		User user = userRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);

		// 유저는 있지만 Pet 정보가 없는 경우 (404 예외)
		Pet pet = petRepository.findByUserId(userId)
			.orElseThrow(PetNotFoundException::new);

		// 본인 반려견인지 확인 (403 예외)
		if (!pet.getUser().getId().equals(userId)) {
			throw new PetException(HttpStatus.FORBIDDEN, PetExceptionType.REQUIRED_PERMISSION.getMessage());
		}

		// 필수 값 검증
		if (name == null || name.isBlank()) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_NAME.getMessage());
		}
		if (!name.matches("^[a-zA-Z가-힣]+$")) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_NAME_FORMAT.getMessage());
		}
		if (name.length() > 10) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_NAME_LENGTH.getMessage());
		}
		if (age == null) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_AGE.getMessage());
		}
		if (age <= 0 || age > 50) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_AGE_VALUE.getMessage());
		}
		if (weight == null) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_WEIGHT.getMessage());
		}
		if (weight < 0.1 || weight > 200.0) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_WEIGHT.getMessage());
		}
		if (breed == null || breed.isBlank()) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_BREED.getMessage());
		}

		// ✅ petProfileImage request에서 가져오지 않고 직접 MultipartFile에서 처리
		String petProfileImageUrl = (petProfileImage != null && !petProfileImage.isEmpty()) ? saveFile(petProfileImage) : null;

		pet.updatePet(
			name,
			petProfileImageUrl,  // ✅ 여기서 MultipartFile → String URL 변환
			age,
			gender,
			breed,
			weight
		);
		return PetResponse.from(pet);
	}

	// 반려견 정보 삭제
	@Transactional
	public void deletePet(Integer userId) {
		// 사용자 존재 여부 확인 (없으면 404 예외)
		User user = userRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);

		// 반려견 존재 여부 확인 (없으면 404 예외)
		Pet pet = petRepository.findByUserId(userId)
			.orElseThrow(PetNotFoundException::new);

		// 본인 반려견인지 확인 (403 예외)
		if (!pet.getUser().getId().equals(userId)) {
			throw new PetException(HttpStatus.FORBIDDEN, PetExceptionType.REQUIRED_PERMISSION.getMessage());
		}

		pet.onSoftDelete();
		petRepository.save(pet);
	}
}
