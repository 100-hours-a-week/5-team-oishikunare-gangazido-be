package org.example.gangazido_be.map.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name="marker") //테이블명 명시
public class MarkerEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // Auto Increment 적용
    private int id;  // 마커 ID (Primary Key)

	@Column(nullable = false)
    private int user_id;  // 마커를 등록한 사용자 ID

    @Column(nullable = false)
    private int type;  // 마커 타입 (0: 댕플, 1: 들개, 2: 빙판길, 3: 염화칼슘, 4: 공사중)

    @Column(nullable = false)
    private Double latitude;  // 위도 좌표

    @Column(nullable = false)
    private Double longitude;  // 경도 좌표

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	public MarkerEntity(int user_id, int type, double latitude, double longitude) {
        this.user_id = user_id;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = LocalDateTime.now(); // 현재 시간 저장
    }
}
