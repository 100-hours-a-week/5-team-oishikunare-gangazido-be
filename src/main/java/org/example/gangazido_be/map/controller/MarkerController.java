package org.example.gangazido_be.map.controller;

import jakarta.validation.Valid;
import org.example.gangazido_be.map.dto.MarkerRequestDto;
import org.example.gangazido_be.map.dto.MarkerResponseDto;
import org.example.gangazido_be.map.service.MarkerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController	// 이 클래스가 컨트롤러라는 것을 알림
@RequestMapping("v1/markers")	// 기본 url 설정
public class MarkerController {
	private final MarkerService markerService;

	// MarkerController 객체 생성 시 markerService 변수에 넣음.
	public MarkerController(MarkerService markerService) {
        this.markerService = markerService;
    }

	/**
     * 마커 등록 API
     * @param requestDto - 클라이언트에서 보내온 마커 정보
     * @return 성공 메시지와 등록된 마커 정보
     */
	@PostMapping	// HTTP POST 요청 처리
    public ResponseEntity<?> createMarker(@Valid @RequestBody MarkerRequestDto requestDto) {
		// 위도, 경도 값이 누락된 경우 예외 발생
		// 값이 없을 경우 ILLegalStateException 발생
        if (requestDto.getLatitude() == null || requestDto.getLongitude() == null) {
            throw new IllegalStateException("invalid_latitude_longitude");	// MarkerExceptionHandler로 넘기기
        }

		// user_id 없을 시 SecurityException 발생
		if (requestDto.getUser_id() == null) {
			throw new SecurityException(("required_authorization"));
		}

		// 마커 저장 로직 호출
		// requestDto를 서비스 계층으로 넘겨 마커를 저장, 저장된 마커 정보를 반환받는다.
		MarkerResponseDto responseDto = markerService.createMarker(requestDto);

		// 정상적으로 저장된 경우
		// 응답 데이터는 JSON 형태로 클라이언트에 전달
        Map<String, Object> successResponse = new LinkedHashMap<>();
        successResponse.put("message", "marker_registered_success"); // 성공 메시지
        successResponse.put("data", responseDto); // 저장된 마커 데이터

		// 200 ok 응답 반환
        return ResponseEntity.ok(successResponse);
	}
}
