package org.example.gangazido_be.pet.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

// 테스트용 더미
public class User {

	@Id
	private Long id; // 사용자 PK (임시)

	private String username; // 사용자 이름 (임시 더미 값)

	// 필요 시 더미 필드 추가
}
