package org.example.gangazido_be.user.repository;

import org.example.gangazido_be.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
	Optional<User> findByEmail(String email);

	Optional<User> findByNickname(String nickname);

	// 탈퇴하지 않은 사용자만 이메일로 검색, Spring Data JPA가 메서드 이름을 분석해서 자동으로 쿼리를 생성
	Optional<User> findByEmailAndDeletedAtIsNull(String email);

	Optional<User> findByNicknameAndDeletedAtIsNull(String nickname);

	Optional<User> findByIdAndDeletedAtIsNull(Integer id);
}
