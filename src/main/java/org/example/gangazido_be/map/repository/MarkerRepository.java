package org.example.gangazido_be.map.repository;

import org.example.gangazido_be.map.entity.MarkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarkerRepository extends JpaRepository<MarkerEntity, Long> {
	// save()는 JpaRepository에서 제공
}
