package org.example.gangazido_be.pet.entity;

import jakarta.persistence.*;
import lombok.*;

import org.example.gangazido_be.user.entity.User;
import org.hibernate.annotations.SQLRestriction;
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
	private Integer userId; // PK

	@OneToOne	// 한 명의 유저 - 하나의 반려견 정보
	@MapsId		// Pet의 PK가 User의 PK를 그대로 따라간다는 의미 (공유 PK 방식)
	@JoinColumn(name = "user_id")	// FK
	private User user;

	@Column(nullable = false, length = 10)
	private String name;    // 반려견 이름

	private String profileImage;    // 반려견 프로필 이미지 URL

	@Column(nullable = false)
	private Integer age;    // 반려견 나이

	@Column(nullable = false, columnDefinition = "BIT")
	private Boolean gender;        // 반려견 성별 (남: 0, 여: 1)

	@Column(nullable = false, length = 200)
	private String breed;        // 반려견 견종

	@Column(nullable = false)
	private Double weight;        // 반려견 몸무게

	private LocalDateTime createdAt;    // 생성일
	private LocalDateTime updatedAt;    // 수정일
	private LocalDateTime deletedAt;    // 삭제일

	// entity 수정
	public void updatePet(String name, String profileImage, Integer age,
		boolean gender, String breed, Double weight) {
		this.name = name;
		this.profileImage = profileImage;
		this.age = age;
		this.gender = gender;
		this.breed = breed;
		this.weight = weight;
	}

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public void onSoftDelete() {
		this.deletedAt = LocalDateTime.now();
	}

}

