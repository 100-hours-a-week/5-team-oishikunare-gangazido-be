package org.example.gangazido_be.pet.repository;

import org.example.gangazido_be.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {
	// @Where가 적용되어 있으므로, 삭제되지 않은 데이터만 조회됨
	Optional<Pet> findByUserId(Long userId);

	boolean existsByUserId(Long userId);
}

