package org.example.gangazido_be.pet.service;

import lombok.RequiredArgsConstructor;

import org.example.gangazido_be.pet.dto.PetCreateRequest;
import org.example.gangazido_be.pet.dto.PetResponse;
import org.example.gangazido_be.pet.entity.Pet;
import org.example.gangazido_be.pet.entity.User;
import org.example.gangazido_be.pet.exception.PetException;
import org.example.gangazido_be.pet.exception.PetExceptionType;
import org.example.gangazido_be.pet.exception.PetNotFoundException;
import org.example.gangazido_be.pet.exception.UserNotFoundException;
import org.example.gangazido_be.pet.repository.PetRepository;
import org.example.gangazido_be.pet.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PetService {
	private final PetRepository petRepository;
	private final UserRepository userRepository;

	// 반려견 정보 등록
	@Transactional
	public PetResponse createPet(Long userId, PetCreateRequest request) {
		// 사용자 존재 여부 확인 (없으면 404  예외)
		User user = userRepository.findById(userId) // userId로 User 엔티티 조회
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

		// 엔티티 생성 및 저장
		Pet pet = Pet.builder()
			.user(user) // 엔티티 자체 전달
			.name(request.getName())
			.profileImage(request.getProfileImage())
			.age(request.getAge())
			.gender(request.getGender())
			.breed(request.getBreed())
			.weight(request.getWeight())
			.createdAt(LocalDateTime.now())
			.build();

		Pet savedPet = petRepository.save(pet);
		return PetResponse.from(savedPet);
	}

	// 반려견 정보 조회
	@Transactional(readOnly = true)
	public PetResponse getPet(Long userId) {
		// 사용자가 존재하는지 확인 (없으면 404 예외)
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_USER.getMessage()));

		// 해당 사용자의 반려견 정보 조회 (없으면 404 예외)
		Pet pet = petRepository.findByUserId(userId)
			.orElseThrow(() -> new PetNotFoundException());

		return PetResponse.from(pet);
	}

	// 반려견 정보 수정
	@Transactional
	public PetResponse updatePet(Long userId, PetCreateRequest request) {
		// 사용자 존재 여부 확인 (없으면 404 예외)
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserNotFoundException());

		// 유저는 있지만 Pet 정보가 없는 경우 (404 예외)
		Pet pet = petRepository.findByUserId(userId)
			.orElseThrow(() -> new PetNotFoundException());

		// 본인 반려견인지 확인 (403 예외)
		// if (!pet.getUser().getUserId().equals(userId)) {
		// 	throw new PetException(HttpStatus.FORBIDDEN, PetExceptionType.REQUIRED_PERMISSION.getMessage());
		// }

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

		pet.updatePet(
			request.getName(),
			request.getProfileImage(),
			request.getAge(),
			request.getGender(),
			request.getBreed(),
			request.getWeight()
		);

		return PetResponse.from(pet);
	}

	// 반려견 정보 삭제
	@Transactional
	public void deletePet(Long userId) {
		// 1. 먼저 유저가 존재하는지 확인
		if (!userRepository.existsById(userId)) {
			throw new UserNotFoundException();
		}
		// 2. 유저는 있지만 Pet 정보가 없는 경우
		Pet pet = petRepository.findByUserId(userId)
			.orElseThrow(() -> new PetNotFoundException());

		pet.softDelete();
	}
}
