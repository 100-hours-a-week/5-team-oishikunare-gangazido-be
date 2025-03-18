//PetRepository
package org.example.gangazido_be.gpt.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.example.gangazido_be.gpt.model.Pet; // ✅ Pet import 추가 확인

@Repository
public interface PetRepository extends JpaRepository<Pet, Integer> {
	List<Pet> findByUserId(int userId);
}
