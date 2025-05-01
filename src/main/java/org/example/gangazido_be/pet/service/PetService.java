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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetService {
	private final PetRepository petRepository;
	private final UserRepository userRepository;

	private static final String CLOUDFRONT_URL = "https://d3jeniacjnodv5.cloudfront.net";	// 배포 url
	//private static final String CLOUDFRONT_URL = "https://d2zi61xwrfrt4q.cloudfront.net";

	// 반려견 정보 등록
	@Transactional
	public PetResponse createPet(Integer userId, String name, Integer age, Boolean gender, String breed, Double weight, String profileImage) {
		log.debug("📸 받은 profileImage 파라미터: {}", profileImage);

		User user = userRepository.findByIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_USER.getMessage()));

		if (petRepository.existsByUserIdAndDeletedAtIsNull(userId)) {
			throw new PetException(HttpStatus.CONFLICT, PetExceptionType.ALREADY_EXISTS_PET.getMessage());
		}

		validatePetFields(name, age, gender, breed, weight);

		Pet deletedPet = petRepository.findByUserIdIncludingDeleted(userId)
			.filter(p -> p.getDeletedAt() != null)
			.orElse(null);

		if (deletedPet != null) {
			log.debug("✔️ 소프트 삭제된 반려견 복구 로직 실행됨");
			deletedPet.setDeletedAt(null);
			deletedPet.setUser(user);
			deletedPet.updatePet(name, profileImage, age, gender, breed, weight);

			Pet saved = petRepository.save(deletedPet);
			return buildPetResponseWithImageUrl(saved);
		}

		Pet pet = Pet.builder()
			.user(user)
			.name(name)
			.profileImage(profileImage)
			.age(age)
			.gender(gender)
			.breed(breed)
			.weight(weight)
			.build();

		Pet saved = petRepository.save(pet);
		return buildPetResponseWithImageUrl(saved);
	}

	// 반려견 정보 조회
	@Transactional(readOnly = true)
	public PetResponse getPet(Integer userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_USER.getMessage()));

		Pet pet = petRepository.findByUserId(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_PET.getMessage()));

		return buildPetResponseWithImageUrl(pet);
	}

	// 반려견 정보 수정
	@Transactional
	public PetResponse updatePet(Integer userId, String name, Integer age, Boolean gender, String breed, Double weight, String profileImage) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_USER.getMessage()));

		Pet pet = petRepository.findByUserId(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_PET.getMessage()));

		if (!pet.getUser().getId().equals(userId)) {
			throw new PetException(HttpStatus.FORBIDDEN, PetExceptionType.REQUIRED_PERMISSION.getMessage());
		}

		validatePetFields(name, age, gender, breed, weight);

		String profileImagePath = profileImage == null ? null : profileImage;

		pet.updatePet(name, profileImagePath, age, gender, breed, weight);
		Pet saved = petRepository.save(pet);

		return buildPetResponseWithImageUrl(saved);
	}

	// 반려견 정보 삭제
	@Transactional
	public void deletePet(Integer userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_USER.getMessage()));

		Pet pet = petRepository.findByUserId(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_PET.getMessage()));

		if (!pet.getUser().getId().equals(userId)) {
			throw new PetException(HttpStatus.FORBIDDEN, PetExceptionType.REQUIRED_PERMISSION.getMessage());
		}

		pet.onSoftDelete();
		petRepository.save(pet);
	}

	// 🔒 공통 유효성 검사 메서드
	private void validatePetFields(String name, Integer age, Boolean gender, String breed, Double weight) {
		if (name == null || name.isBlank()) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_NAME.getMessage());
		}
		if (!name.matches("^[a-zA-Z가-힣]+$")) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_NAME_FORMAT.getMessage());
		}
		if (name.length() > 10) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_NAME_LENGTH.getMessage());
		}
		if (age == null || age <= 0 || age > 50) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_AGE_VALUE.getMessage());
		}
		if (weight == null || weight < 0.1 || weight > 200.0) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.INVALID_PET_WEIGHT.getMessage());
		}
		if (gender == null) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_GENDER.getMessage());
		}
		if (breed == null || breed.isBlank()) {
			throw new PetException(HttpStatus.BAD_REQUEST, PetExceptionType.REQUIRED_PET_BREED.getMessage());
		}
	}

	// ✅ CloudFront URL 붙여서 반환하는 공통 메서드
	private PetResponse buildPetResponseWithImageUrl(Pet pet) {
		PetResponse response = PetResponse.from(pet);

		if (pet.getProfileImage() != null && !pet.getProfileImage().isBlank()) {
			String imageUrl = CLOUDFRONT_URL + "/" + pet.getProfileImage() + "?t=" + System.currentTimeMillis();
			response.setProfileImage(imageUrl);
		}

		return response;
	}

	// userId로 반려견 이미지, 이름 보내기
	@Transactional(readOnly = true)
	public PetResponse getPublicPetInfoByUserId(Integer userId) {
		Pet pet = petRepository.findByUserId(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_PET.getMessage()));

		PetResponse response = PetResponse.from(pet);

		// CloudFront URL 붙이기
		if (pet.getProfileImage() != null && !pet.getProfileImage().isBlank()) {
			String imageUrl = CLOUDFRONT_URL + "/" + pet.getProfileImage() + "?t=" + System.currentTimeMillis();
			response.setProfileImage(imageUrl);
		}

		return response;
	}
}
