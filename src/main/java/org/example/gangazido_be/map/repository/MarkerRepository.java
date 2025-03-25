package org.example.gangazido_be.map.repository;

import org.example.gangazido_be.map.entity.MarkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarkerRepository extends JpaRepository<MarkerEntity, UUID> {
	// JpaRepository 통해 CRUD 기능 자동으로 가짐

	// 위도/경도를 기준으로 반경(radius) 내 마커 조회 하버사인 공식, 두 좌표 사이의 거리 계산을 위한 공식
	@Query(value = "SELECT * FROM marker " +
		"WHERE (6371 * acos(cos(radians(:latitude)) * cos(radians(latitude)) " +
		"* cos(radians(longitude) - radians(:longitude)) + sin(radians(:latitude)) * sin(radians(latitude)))) <= :radius",
		nativeQuery = true)
	List<MarkerEntity> findMarkersWithinRadius(
		@Param("latitude") double latitude,
		@Param("longitude") double longitude,
		@Param("radius") double radius
	);
}

