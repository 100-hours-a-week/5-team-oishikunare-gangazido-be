package org.example.gangazido_be.map.service;

import org.example.gangazido_be.map.dto.MarkerRequestDto;
import org.example.gangazido_be.map.dto.MarkerResponseDto;
import org.example.gangazido_be.map.entity.MarkerEntity;
import org.example.gangazido_be.map.repository.MarkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MarkerService {
	private final MarkerRepository markerRepository; // 데이터베이스와 연결할 레포지토리

	public MarkerService(MarkerRepository markerRepository) {
		this.markerRepository = markerRepository;
	}

	// 트랜잭션, DB 저장 문제 생기면 롤백
	@Transactional
	public MarkerResponseDto createMarker(MarkerRequestDto requestDto) {

		// 1️⃣ DTO → 엔티티 변환 (DB 저장을 위해)
		MarkerEntity markerEntity = new MarkerEntity(
			UUID.randomUUID(),    // UUID
			requestDto.getUser_id(),
			requestDto.getType(),
			requestDto.getLatitude(),
			requestDto.getLongitude()
		);

		// 2️⃣ 데이터베이스에 마커 저장
		MarkerEntity savedMarker = markerRepository.save(markerEntity);

		// 3️⃣ 저장된 데이터를 ResponseDto로 변환하여 반환
		return new MarkerResponseDto(
			savedMarker.getId(),
			savedMarker.getUser_id(),
			savedMarker.getType(),
			savedMarker.getLatitude(),
			savedMarker.getLongitude(),
			savedMarker.getCreatedAt().toString()
		);
	}

	// 마커 삭제
	@Transactional
	public void deleteMarker(UUID markerId, Integer sessionUserId) {
		// 존재하는지 확인 후 엔티티 조회
		MarkerEntity marker = markerRepository.findById(markerId)
			.orElseThrow(() -> new IllegalArgumentException("marker_not_found"));

		// 마커 등록자와 현재 로그인 유저 비교
		if (!marker.getUserId().equals(sessionUserId)) {
			throw new SecurityException("required_permission"); // 예외 던지기 (403)
		}

		// 마커 삭제
		markerRepository.deleteById(markerId);
	}

	// 반경 내 마커 조회
	@Transactional(readOnly = true)
	public List<MarkerResponseDto> findMarkersWithinRadius(double latitude, double longitude, double radius) {
		// DB에서 반경 내 마커를 조회
		List<MarkerEntity> markers = markerRepository.findMarkersWithinRadius(latitude, longitude, radius);

		// 조회된 엔티티 리스트를 DTO 리스트로 변환
		return markers.stream()
			.map(marker -> new MarkerResponseDto(
				marker.getId(),
				marker.getUser_id(),
				marker.getType(),
				marker.getLatitude(),
				marker.getLongitude(),
				marker.getCreatedAt().toString()
			))
			.toList();
	}

	// 특정 마커 조회 (id(UUID) 기반)
	@Transactional(readOnly = true)
	public MarkerResponseDto getMarkerById(UUID Id) {
		MarkerEntity marker = markerRepository.findById(Id)
			.orElseThrow(() -> new IllegalArgumentException("marker_not_found")); // 마커 없으면 전역 예외처리로 던짐

		return new MarkerResponseDto(
			marker.getId(),
			marker.getUser_id(),
			marker.getType(),
			marker.getLatitude(),
			marker.getLongitude(),
			marker.getCreatedAt().toString()
		);
	}
}
