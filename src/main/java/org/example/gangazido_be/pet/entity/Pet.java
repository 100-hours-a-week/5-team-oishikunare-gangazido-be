package org.example.gangazido_be.pet.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "pet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class Pet {
	@Id
	private Long userId; // PK이자 FK로 사용

	@OneToOne
	@MapsId
	@JoinColumn(name = "user_id")
	private User user;

	@Column(nullable = false, length = 10)
	private String name;    // 반려견 이름

	private String profileImage;    // 반려견 프로필 이미지 URL

	@Column(nullable = false)
	private Integer age;    // 반려견 나이

	@Column(nullable = false)
	private Integer gender;        // 반려견 성별 (남: 0, 여: 1)

	@Column(nullable = false)
	private String breed;        // 반려견 견종

	@Column(nullable = false)
	private Double weight;        // 반려견 몸무게

	private LocalDateTime createdAt;    // 생성일
	private LocalDateTime updatedAt;    // 수정일
	private LocalDateTime deletedAt;    // 삭제일

	// entity 수정
	public void updatePet(String name, String profileImage, Integer age,
		Integer gender, String breed, Double weight) {
		this.name = name;
		this.profileImage = profileImage;
		this.age = age;
		this.gender = gender;
		this.breed = breed;
		this.weight = weight;
		this.updatedAt = LocalDateTime.now();
	}

	// 소프트 딜리트 처리
	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
	}

}

