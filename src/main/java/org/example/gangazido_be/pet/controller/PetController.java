package org.example.gangazido_be.pet.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.example.gangazido_be.pet.common.ApiResponse;
import org.example.gangazido_be.pet.dto.PetCreateRequest;
import org.example.gangazido_be.pet.dto.PetResponse;
import org.example.gangazido_be.pet.service.PetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/pets/me")
@Validated
public class PetController {
	private final PetService petService;

	// ✅ 유저 없어서 테스트용
	@PostMapping("/test-login")
	public ResponseEntity<ApiResponse> testLogin(HttpSession session) {
		session.setAttribute("userId", 4L); // 테스트용 userId 설정
		return ResponseEntity.ok(ApiResponse.of("test_login_success", null));
	}

	// 반려견 정보 등록 (POST)
	@PostMapping
	public ResponseEntity<ApiResponse> createPet(@Valid @RequestBody PetCreateRequest request, HttpSession session) {
		// 세션에서 userId 가져오기
		Long userId = (Long)session.getAttribute("userId");

		// 세션에 userId가 없으면 401 Unauthorized 응답
		if (userId == null) {
			return ResponseEntity.status(401).body(ApiResponse.of("required_authorization", null));
		}

		PetResponse petResponse = petService.createPet(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("create_pet_success", petResponse));
	}

	// 반려견 정보 조회 (GET)
	@GetMapping
	public ResponseEntity<ApiResponse> getPet(HttpSession session) {
		// 세션에서 userId 가져오기
		Long userId = (Long)session.getAttribute("userId");

		// 세션이 userId가 없으면 401 Unauthorized
		if (userId == null) {
			return ResponseEntity.status(401).body(ApiResponse.of("required_authorization", null));
		}

		PetResponse petResponse = petService.getPet(userId);
		return ResponseEntity.ok(ApiResponse.of("get_pet_success", petResponse));
	}

	// 반려견 정보 수정 (PATCH)
	@PatchMapping
	public ResponseEntity<ApiResponse> updatePet(@RequestBody PetCreateRequest request, HttpSession session) {
		// 세션에서 userId 가져오기
		Long userId = (Long) session.getAttribute("userId");

		// 세션에 userId가 없으면 401 Unauthorized 응답
		if (userId == null) {
			return ResponseEntity.status(401).body(ApiResponse.of("required_authorization", null));
		}

		PetResponse petResponse = petService.updatePet(userId, request);
		return ResponseEntity.status(HttpStatus.OK)
			.body(ApiResponse.of("change_pet_success", petResponse));
	}

	// 반려견 정보 삭제 (DELETE)
	@DeleteMapping
	public ResponseEntity<ApiResponse> deletePet(HttpSession session) {
		// 세션에서 userId 가져오기
		Long userId = (Long) session.getAttribute("userId");

		// 세션이 없으면 401 응답
		if (userId == null) {
			return ResponseEntity.status(401).body(ApiResponse.of("required_authorization", null));
		}
		petService.deletePet(userId);
		return ResponseEntity.ok(ApiResponse.of("delete_pet_success", null));
	}
}
