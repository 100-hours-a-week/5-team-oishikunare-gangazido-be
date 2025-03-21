//PetRepository
// ✅ PetRepository: Pet 엔티티와 DB를 연결해주는 인터페이스 (Spring Data JPA 사용)
package org.example.gangazido_be.gpt.model;

import org.springframework.data.jpa.repository.JpaRepository; // jpa 기본 기능 제공
import org.springframework.stereotype.Repository; // 레포지토리 컴포넌트로 등록
import java.util.List;
import org.example.gangazido_be.gpt.model.Pet; // ✅ Pet import 추가 확인

@Repository // 스프링이 이 인터페이스를 레포지토리로 인식하게 함 (의존성 주입 가능)
public interface PetRepository extends JpaRepository<Pet, Integer> {
	//userID를 기준으로 Pet 목록 조회
	List<Pet> findByUserId(int userId); // 사용자 id로 반려동물 리스트를 가져오는 커스텀 쿼리 메서드
}


