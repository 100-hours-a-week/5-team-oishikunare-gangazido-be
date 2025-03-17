package org.example.gangazido_be.map.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 마커 등록 응답 (Server > Client)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarkerResponseDto {
	private int marker_id;      // 등록된 마커의 ID
    private int user_id;        // 마커를 등록한 사용자 ID
    private int marker_type;     // 마커 타입 (0~4)
    private double latitude;    // 위도 좌표
    private double longitude;   // 경도 좌표
    private String createdTime; // 마커 생성 시간
}
