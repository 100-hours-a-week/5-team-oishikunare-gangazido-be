package org.example.gangazido_be.map.service;

import org.example.gangazido_be.map.dto.MarkerRequestDto;
import org.example.gangazido_be.map.dto.MarkerResponseDto;
import org.example.gangazido_be.map.entity.MarkerEntity;
import org.example.gangazido_be.map.repository.MarkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
