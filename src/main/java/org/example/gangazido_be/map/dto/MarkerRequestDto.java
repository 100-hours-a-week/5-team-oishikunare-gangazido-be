package org.example.gangazido_be.map.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarkerRequestDto {
	@NotNull(message = "required_authorization")
    private Integer user_id;   // 유저 아이디 (UUID)

	@NotNull(message = "invalid_marker_type")
    private Integer type;   // 마커 타입 (0: 댕플, 1: 들개, 2: 빙판길, 3: 염화칼슘, 4: 공사중)

    @NotNull(message = "invalid_latitude")
    private Double latitude;  // 마커의 위도 좌표

    @NotNull(message = "invalid_longitude")
    private Double longitude; // 마커의 경도 좌표
}
