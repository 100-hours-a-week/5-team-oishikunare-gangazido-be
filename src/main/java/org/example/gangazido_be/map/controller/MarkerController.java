package org.example.gangazido_be.map.controller;

import java.util.*;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.example.gangazido_be.map.dto.MarkerRequestDto;
import org.example.gangazido_be.map.dto.MarkerResponseDto;
import org.example.gangazido_be.map.service.MarkerService;
import org.example.gangazido_be.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController    // 이 클래스가 컨트롤러라는 것을 알림
@RequestMapping("v1/markers")    // 기본 url 설정
public class MarkerController {
	private final MarkerService markerService;

	// MarkerController 객체 생성 시 markerService 변수에 넣음.
	public MarkerController(MarkerService markerService) {
		this.markerService = markerService;
	}

	// 마커 등록 API
	@PostMapping    // POST 마커 등록 요청 처리
	public ResponseEntity<?> createMarker(
		HttpSession session,	// 현재 세션에서 로그인 정보 가져옴
		@RequestBody MarkerRequestDto requestDto) {	// 클라이언트가 보낸 마커 데이터 (JSON > DTO)
		Object userObj = session.getAttribute("user");
		//
		if (userObj == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("message", "required_authorization", "data", new HashMap<>()));
		}

		// User 객체에서 ID 추출
		User user = (User) userObj;
		Integer sessionUserId = user.getId();
		System.out.println("세션 user_id: " + sessionUserId); // 콘솔 디버깅

		// 마커 저장 로직 호출
		// requestDto를 서비스 계층으로 넘겨 마커를 저장, 저장된 마커 정보를 반환받는다.
		MarkerResponseDto responseDto = markerService.createMarker(sessionUserId, requestDto);
		// 위도, 경도 값이 누락된 경우 예외 발생
		// 값이 없을 경우 ILLegalStateException 발생
		if (requestDto.getLatitude() == null || requestDto.getLongitude() == null) {
			throw new IllegalStateException("invalid_latitude_longitude");    // MarkerExceptionHandler로 넘기기
		}

		// 정상적으로 저장된 경우
		// 응답 데이터는 JSON 형태로 클라이언트에 전달
		Map<String, Object> successResponse = new LinkedHashMap<>();
		successResponse.put("message", "marker_registered_success"); // 성공 메시지
		successResponse.put("data", responseDto); // 저장된 마커 데이터

		// 200 ok 응답 반환
		return ResponseEntity.ok(successResponse);
	}

	@DeleteMapping("/{Id}")    // DELETE 마커 삭제 요청 처리
	public ResponseEntity<?> deleteMarker(
		@PathVariable String Id, // URL의 ID 수신
		HttpSession session	// 세션에서 로그인 된 사용자 정보 가져오기
	) {
		// UUID 변환 (유효하지 않은 UUID 처리)
		UUID markerId;
		try {
			markerId = UUID.fromString(Id);	// 가져온 String Id를 UUID로 변환
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("message", "invalid_marker_id", "data", new HashMap<>()));
		}

		// 세션에서 user_id 가져오기 (로그인 상태 확인)
		Object userObj = session.getAttribute("user");

		if (userObj == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Map.of("message", "required_authorization", "data", new HashMap<>()));
		}

		// User 객체에서 ID 추출
		Integer sessionUserId = ((User) userObj).getId();
		System.out.println("세션 user_id: " + sessionUserId); // 콘솔 디버깅

		// 서비스 계층에 마커 삭제 요청 위임, 마커 삭제 진행
		markerService.deleteMarker(markerId, sessionUserId);

		// 성공적으로 삭제할 경우 응답 반환
		return ResponseEntity.ok(Map.of("data", new HashMap<>(), "message", "marker_deleted_success"));
	}

	@GetMapping
	public ResponseEntity<?> getMarkers(@RequestParam("latitude") double latitude, @RequestParam("longitude") double longitude, @RequestParam(value = "radius", defaultValue = "5.0") double radius) {

	// 위도/경도 범위 검증
	//        if (latitude < 33.1 || latitude > 38.7 || longitude < 125.0 || longitude > 132.0) {
	//            return ResponseEntity.badRequest().body(Map.of("message", "invalid_request", "data", null));
	//        }

		// 마커 조회 실행
		List<MarkerResponseDto> markers = markerService.findMarkersWithinRadius(latitude, longitude, radius);

		// 응답 데이터 구성
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("message", "map_data_retrieved_success");

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("latitude", latitude);
		data.put("longitude", longitude);
		data.put("markers", markers);

		response.put("data", data);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/{Id}")
	public ResponseEntity<?> getMarkerById(@PathVariable String Id) {
		UUID markerId = UUID.fromString(Id); // UUID 형식이 잘못되면 자동으로 IllegalArgumentException 발생
		// 마커 정보 조회
		MarkerResponseDto responseDto = markerService.getMarkerById(markerId);

		// 응답 데이터 구성
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", "map_data_retrieved_success");
		data.put("data", responseDto);

		return ResponseEntity.ok(data);
	}
}
