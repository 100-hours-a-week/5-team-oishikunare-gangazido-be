package org.example.gangazido_be.pet.service;

import java.io.File;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.gangazido_be.pet.dto.PetResponse;
import org.example.gangazido_be.pet.entity.Pet;
import org.example.gangazido_be.pet.exception.PetException;
import org.example.gangazido_be.pet.exception.PetExceptionType;
import org.example.gangazido_be.pet.repository.PetRepository;
import org.example.gangazido_be.user.entity.User;
import org.example.gangazido_be.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetService {
	private final PetRepository petRepository;
	private final UserRepository userRepository;

	// 반려견 정보 등록
	@Transactional
	public PetResponse createPet(Integer userId, String name, Integer age, Boolean gender, String breed, Double weight, String profileImage) {
		// 사용자 존재 여부 확인 (없으면 404  예외)
		User user = userRepository.findByIdAndDeletedAtIsNull(userId) // userId로 User 엔티티 조회
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_USER.getMessage()));

		// 이미 해당 userId로 pet이 존재한다면 예외 처리
		if (petRepository.existsByUserIdAndDeletedAtIsNull(userId)) {
			throw new PetException(HttpStatus.CONFLICT, PetExceptionType.ALREADY_EXISTS_PET.getMessage());
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

		String profileImagePath = null;

		// soft-delete된 반려견 있는지 확인
		Pet deletedPet = petRepository.findByUserIdIncludingDeleted(userId)
			.filter(p -> p.getDeletedAt() != null)
			.orElse(null);

		if (deletedPet != null) {
			log.debug("✔️ 소프트 삭제된 반려견 복구 로직 실행됨");

			deletedPet.setDeletedAt(null);
			deletedPet.setUser(user);
			deletedPet.updatePet(name, profileImagePath, age, gender, breed, weight);
			return PetResponse.from(petRepository.save(deletedPet));
		}

		// 엔티티 생성 및 저장
		Pet pet = Pet.builder()
			.user(user)
			.name(name)
			.profileImage(profileImage) // 그냥 S3 URL 그대로 저장
			.age(age)
			.gender(gender)
			.breed(breed)
			.weight(weight)
			.build();

		return PetResponse.from(petRepository.save(pet));
	}

	// 반려견 정보 조회
	@Transactional(readOnly = true)
	public PetResponse getPet(Integer userId) {
		// 사용자가 존재하는지 확인 (없으면 404 예외)
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_USER.getMessage()));

		// 해당 사용자의 반려견 정보 조회 (없으면 404 예외)
		Pet pet = petRepository.findByUserId(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_PET.getMessage()));

		return PetResponse.from(pet);
	}

	// 반려견 정보 수정
	@Transactional
	public PetResponse updatePet(Integer userId,  String name, Integer age, Boolean gender, String breed, Double weight, String profileImage) {
		// 사용자 존재 여부 확인 (없으면 404 예외)
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_USER.getMessage()));

		// 유저는 있지만 Pet 정보가 없는 경우 (404 예외)
		Pet pet = petRepository.findByUserId(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_PET.getMessage()));

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

		// 수정
		String profileImagePath = profileImage != null && !profileImage.isBlank() ? profileImage : pet.getProfileImage();  // 새 이미지 없으면 기존 유지

		pet.updatePet(name, profileImagePath, age, gender, breed, weight);
		return PetResponse.from(pet);
	}

	// 반려견 정보 삭제
	@Transactional
	public void deletePet(Integer userId) {
		// 사용자 존재 여부 확인 (없으면 404 예외)
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_USER.getMessage()));

		// 반려견 존재 여부 확인 (없으면 404 예외)
		Pet pet = petRepository.findByUserId(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_PET.getMessage()));

		// 본인 반려견인지 확인 (403 예외)
		if (!pet.getUser().getId().equals(userId)) {
			throw new PetException(HttpStatus.FORBIDDEN, PetExceptionType.REQUIRED_PERMISSION.getMessage());
		}

		pet.onSoftDelete();
		petRepository.save(pet);
	}
}
