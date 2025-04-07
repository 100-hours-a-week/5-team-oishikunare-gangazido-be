package org.example.gangazido_be.map.service;

import org.example.gangazido_be.map.dto.MarkerRequestDto;
import org.example.gangazido_be.map.dto.MarkerResponseDto;
import org.example.gangazido_be.map.entity.MarkerEntity;
import org.example.gangazido_be.map.repository.MarkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
	public MarkerResponseDto createMarker(Integer userId, MarkerRequestDto requestDto) {

		// 마커 위경도 중복 위치 확인
		boolean exists = markerRepository.existsAtLocation(
			requestDto.getLatitude(),
			requestDto.getLongitude()
		);
		if (exists) {
			throw new IllegalArgumentException("duplicate_location");
		}

		// 현재 시간 기준 1시간 전 시간 계산
		LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

		// 최근 1시간 동안 마커 개수 조회
		long recentCount = markerRepository.countMarkersInLastHour(userId, oneHourAgo);
		if (recentCount >= 30) {
			throw new IllegalStateException("1시간에 최대 10개의 마커만 등록할 수 있습니다.");
		}

		// 거리 제한 체크
		List<MarkerEntity> nearbyMarkers = markerRepository.findMarkersWithinRadius(
			requestDto.getLatitude(), requestDto.getLongitude(), 1.0 // 최대 거리: 1m
		);
		for (MarkerEntity marker : nearbyMarkers) {
			double distance = calculateDistance(
				requestDto.getLatitude(), requestDto.getLongitude(),
				marker.getLatitude(), marker.getLongitude()
			);

			int newType = requestDto.getType();
			int existingType = marker.getType();

			if (newType == 0 && existingType == 0 && distance < 2.0) {
				// 댕플끼리 2m 이내
				throw new IllegalArgumentException("too_close_dangple");
			} else if (newType >= 1 && existingType >= 1 && distance < 5.0) {
				// 댕져러스끼리 5m 이내
				throw new IllegalArgumentException("too_close_dangerous");
			} else if (
				((newType == 0 && existingType >= 1) || (newType >= 1 && existingType == 0)) && distance < 5.0
			) {
				// 댕플 <-> 댕져러스 3m 이내
				throw new IllegalArgumentException("too_close_mixed");
			}
		}

		// 1️⃣ DTO → 엔티티 객체로 변환 (DB 저장을 위해)
		MarkerEntity markerEntity = new MarkerEntity(
			UUID.randomUUID(),    // UUID 생성
			userId,
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

	// 하버사인 공식 (지구 위도경도로 거리 계산)
	private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
		final int EARTH_RADIUS = 6371000; // meters

		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
			* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS * c;
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
		// markerRepository.deleteById(markerId); 소프트 삭제 처리 위해
		marker.setDeletedAt(LocalDateTime.now());
		markerRepository.save(marker);
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

	// 사용자 ID로 마커를 모두 삭제하는 메서드 - 잭
	@Transactional
	public void deleteAllMarkersByUserId(Integer userId) {
		markerRepository.softDeleteAllByUserId(userId);
	}
}
