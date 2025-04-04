package org.example.gangazido_be.map.repository;

import org.example.gangazido_be.map.entity.MarkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MarkerRepository extends JpaRepository<MarkerEntity, UUID> {
	// JpaRepository 통해 CRUD 기능 자동으로 가짐

	// 위도/경도를 기준으로 반경(radius) 내 마커 조회 하버사인 공식, 두 좌표 사이의 거리 계산을 위한 공식
	@Query(value = "SELECT * FROM marker " +
		"WHERE deleted_at IS NULL AND" +
		"(6371 * acos(cos(radians(:latitude)) * cos(radians(latitude)) " +
		"* cos(radians(longitude) - radians(:longitude)) + sin(radians(:latitude)) * sin(radians(latitude)))) <= :radius",
		nativeQuery = true)
	List<MarkerEntity> findMarkersWithinRadius(
		@Param("latitude") double latitude,
		@Param("longitude") double longitude,
		@Param("radius") double radius
	);

	// 사용자 ID를 기준으로 마커 소프트 딜리트 - 잭
	@Modifying
	@Query(value = "UPDATE marker SET deleted_at = NOW() WHERE user_id = :userId AND deleted_at IS NULL", nativeQuery = true)
	void softDeleteAllByUserId(@Param("userId") Integer userId);

	// 사용자별 마커 갯수 제한
	@Query("SELECT COUNT(m) FROM MarkerEntity m " +
		"WHERE m.user_id = :userId AND m.createdAt >= :oneHourAgo AND m.deletedAt IS NULL")
	long countMarkersInLastHour(@Param("userId") Integer userId, @Param("oneHourAgo") LocalDateTime oneHourAgo);
}

