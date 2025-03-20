//Pet
package org.example.gangazido_be.gpt.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity  // ✅ JPA 엔티티로 인식
@Table(name = "pet")  // ✅ 테이블명 확인
public class Pet {

	@Id  // ✅ JPA에서 기본키 설정
	@Column(name = "user_id") // ✅ 기본키를 user_id로 변경
	private int userId;

	private String name;
	private String profileImage;
	private int age;
	private boolean gender;
	private String breed;
	private float weight;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;


	public int getUserId() {
		return userId;
	}

	public String getName() {
		return name;
	}

	public String getBreed() {
		return breed;
	}

	public int getAge() {
		return age;
	}

	public float getWeight() {
		return weight;
	}
}

