package org.example.gangazido_be.pet.repository;

import org.example.gangazido_be.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Integer> {
	// 삭제되지 않은 반려견만 조회 (기본 동작)
	Optional<Pet> findByUserId(Integer userId);

	// 명시적으로 soft delete 고려 (사용 추천)
	boolean existsByUserIdAndDeletedAtIsNull(Integer userId);

	// soft delete 여부 무시하고 모든 반려견 포함 조회
	@Query(value = "SELECT * FROM pet WHERE user_id = :userId", nativeQuery = true)
	Optional<Pet> findByUserIdIncludingDeleted(@Param("userId") Integer userId);

	// 존재 여부만 확인 (soft delete 고려)
	Optional<Pet> findByUserIdAndDeletedAtIsNull(Integer userId);
}

