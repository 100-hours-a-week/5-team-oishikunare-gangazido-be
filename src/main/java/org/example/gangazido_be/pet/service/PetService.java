package org.example.gangazido_be.pet.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.gangazido_be.pet.dto.PetCreateRequest;
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
	private final String uploadDir; // ✅ `uploadDir`을 생성자로 주입받음

	// ✅ 서버 시작 시 업로드 디렉토리 생성 확인
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
	public PetResponse createPet(Integer userId, PetCreateRequest request, MultipartFile petImage) {
		String imagePath = null;

		// 사용자 존재 여부 확인 (없으면 404  예외)
		User user = userRepository.findByIdAndDeletedAtIsNull(userId) // userId로 User 엔티티 조회
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_USER.getMessage()));

		// 이미 해당 userId로 pet이 존재한다면 예외 처리
		if (petRepository.existsByUserId(userId)) {
			throw new PetException(HttpStatus.CONFLICT, PetExceptionType.ALREADY_EXISTS_PET.getMessage());
		}

		// 필수 값 검증
		if (request.getName() == null || request.getName().isBlank()) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_NAME.getMessage());
		}
		if (!request.getName().matches("^[a-zA-Z가-힣0-9 ]+$")) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_NAME_FORMAT.getMessage());
		}
		if (request.getName().length() > 10) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_NAME_LENGTH.getMessage());
		}
		if (request.getBreed() == null || request.getBreed().isBlank()) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_BREED.getMessage());
		}
		if (request.getAge() <= 0 || request.getAge() > 50) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_AGE_VALUE.getMessage());
		}
		if (request.getWeight() < 0.1 || request.getWeight() > 200.0) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_WEIGHT.getMessage());
		}
		if (request.getGender() == null) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_GENDER.getMessage());
		}
		if (request.getBreed() == null || request.getBreed().isBlank()) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_BREED.getMessage());
		}
		if (request.getAge() <= 0 || request.getAge() > 50) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_AGE_VALUE.getMessage());
		}
		if (request.getWeight() < 0.1 || request.getWeight() > 200.0) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_WEIGHT.getMessage());
		}
		if (request.getGender() == null) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_GENDER.getMessage());
		}

		// ✅ 파일 저장 처리
		if (petImage != null && !petImage.isEmpty()) {
			imagePath = saveFile(petImage);
		}

		// 엔티티 생성 및 저장
		Pet pet = Pet.builder()
			.user(user) // Pet 엔티티가 User 엔티티와 연결되도록
			.name(request.getName())
			.profileImage(imagePath)  // ✅ 저장된 파일 URL 설정
			.age(request.getAge())
			.gender(request.getGender())
			.breed(request.getBreed())
			.weight(request.getWeight())
			.build();

		Pet savedPet = petRepository.save(pet);
		return PetResponse.from(savedPet);
	}

	@Transactional
	public PetResponse updatePetProfileImage(Integer userId, MultipartFile profileImage) {
		Pet pet = petRepository.findByUserId(userId)
			.orElseThrow(PetNotFoundException::new);

		// ✅ 기존 이미지 파일 삭제 (선택적)
		deleteOldFile(pet.getProfileImage());

		// ✅ 새 이미지 저장 후 URL 설정
		String imageUrl = saveFile(profileImage);
		pet.setProfileImage(imageUrl);
		petRepository.save(pet);

		return PetResponse.from(pet);
	}

	private String saveFile(MultipartFile file) {
		if (file == null || file.isEmpty()) return null;

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
	public PetResponse updatePet(Integer userId, PetCreateRequest request, MultipartFile profileImage) {
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
		if (request.getName() == null || request.getName().isBlank()) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_NAME.getMessage());
		}
		if (!request.getName().matches("^[a-zA-Z가-힣]+$")) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_NAME_FORMAT.getMessage());
		}
		if (request.getName().length() > 10) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_NAME_LENGTH.getMessage());
		}
		if (request.getAge() == null) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_AGE.getMessage());
		}
		if (request.getAge() <= 0 || request.getAge() > 50) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_AGE_VALUE.getMessage());
		}
		if (request.getWeight() == null) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_WEIGHT.getMessage());
		}
		if (request.getWeight() < 0.1 || request.getWeight() > 200.0) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_WEIGHT.getMessage());
		}
		if (request.getBreed() == null || request.getBreed().isBlank()) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_BREED.getMessage());
		}

		// ✅ profileImage를 request에서 가져오지 않고 직접 MultipartFile에서 처리
		String imageUrl = (profileImage != null && !profileImage.isEmpty()) ? saveFile(profileImage) : null;

		pet.updatePet(
			request.getName(),
			imageUrl,  // ✅ 여기서 MultipartFile → String URL 변환
			request.getAge(),
			request.getGender(),
			request.getBreed(),
			request.getWeight()
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
