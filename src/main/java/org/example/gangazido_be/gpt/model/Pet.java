// ✅ Pet 엔티티 클래스 (데이터베이스의 'pet' 테이블과 매핑)
package org.example.gangazido_be.gpt.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity  // ✅ 이 클래스가 JPA 엔티티임을 명시 (데이터베이스 테이블과 매핑됨)
@Table(name = "pet")  // ✅ 실제 DB 테이블 이름을 "pet"으로 지정
public class Pet {

	@Id  // ✅ 기본키(Primary Key)로 지정
	@Column(name = "user_id") // ✅ DB 테이블에서 user_id 컬럼과 매핑됨
	private int userId;

	private String name; // 반려동물 이름
	private String profileImage; //  반려동물 프로필 이미지 URL 또는 경로
	private int age; //  반려동물 나이
	private boolean gender;
	private String breed;
	private float weight;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;

	//  Getter 메서드 (필드 값을 외부에서 조회 가능하도록 제공)
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

