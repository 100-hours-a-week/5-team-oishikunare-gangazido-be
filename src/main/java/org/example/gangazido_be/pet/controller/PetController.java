package org.example.gangazido_be.pet.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.example.gangazido_be.pet.common.PetApiResponse;
import org.example.gangazido_be.pet.dto.PetResponse;
import org.example.gangazido_be.pet.service.PetService;
import org.example.gangazido_be.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/pets/me")
@Validated
public class PetController {
	private final PetService petService;

	@PostMapping(value = "/v1/pets/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<PetApiResponse<PetResponse>> createPet(
		@RequestParam("name") String name,
		@RequestParam("age") Integer age,
		@RequestParam("gender") Boolean gender,
		@RequestParam("breed") String breed,
		@RequestParam("weight") double weight,
		@RequestPart(value = "profileImage", required = false) MultipartFile petProfileImage,
		HttpSession session) {
		// 사용자 로그인 상태 확인
		User user = (User) session.getAttribute("user");
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(PetApiResponse.of("required_authorization", null));
		}

		// userId 포함해서 반려견 등록
		PetResponse response = petService.createPet(user.getId(), name, age, gender, breed, weight, petProfileImage);

		return ResponseEntity.ok(PetApiResponse.of("create_pet_success", response));
	}

	// 반려견 정보 조회
	@GetMapping
	public ResponseEntity<PetApiResponse<PetResponse>> getPet(HttpSession session) {
		User user = (User) session.getAttribute("user");  // 세션에서 "user" 객체 가져오기
		Integer userId = null;

		if (user != null) {
			userId = user.getId();  // "user" 객체에서 userId 가져오기
		}

		// 세션에 userId가 없으면 401 Unauthorized 응답
		if (userId == null) {
			return ResponseEntity.status(401).body(PetApiResponse.of("required_authorization", null));
		}

		PetResponse petResponse = petService.getPet(userId);
		return ResponseEntity.ok(PetApiResponse.of("get_pet_success", petResponse));
	}

	// 반려견 정보 수정
	@PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<PetApiResponse<PetResponse>> updatePet(
		@RequestParam("name") String name,
		@RequestParam("age") Integer age,
		@RequestParam("gender") Boolean gender,
		@RequestParam("breed") String breed,
		@RequestParam("weight") double weight,
		@RequestPart(value = "profileImage", required = false) MultipartFile petProfileImage,
		HttpSession session) {

		User user = (User) session.getAttribute("user");
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(PetApiResponse.of("required_authorization", null));
		}
		PetResponse petResponse = petService.updatePet(user.getId(), name, age, gender, breed, weight, petProfileImage);

		return ResponseEntity.ok(PetApiResponse.of("change_pet_success", petResponse));
	}

	// 반려견 정보 삭제
	@DeleteMapping
	public ResponseEntity<PetApiResponse<PetResponse>> deletePet(HttpSession session) {
		User user = (User) session.getAttribute("user");  // 세션에서 "user" 객체 가져오기
		Integer userId = null;

		if (user != null) {
			userId = user.getId();  // "user" 객체에서 userId 가져오기
		}

		// 세션에 userId가 없으면 401 Unauthorized 응답
		if (userId == null) {
			return ResponseEntity.status(401).body(PetApiResponse.of("required_authorization", null));
		}

		petService.deletePet(userId);
		return ResponseEntity.ok(PetApiResponse.of("delete_pet_success", null));
	}
}
