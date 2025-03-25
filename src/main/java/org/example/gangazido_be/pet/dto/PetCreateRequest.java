package org.example.gangazido_be.pet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


/* 지금은 @RequestPart("request") PetCreateRequest 방식이 제대로 안돼서 제외됨 (사용안하는 파일)
* 즉, 클라이언트 요청 파라미터들을 DTO로 받지 않고, 필드 단위로 받는 중 */
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PetCreateRequest {
	@NotBlank(message = "required_pet_name")
	@Size(max = 10, message = "invalid_pet_name_length")
	@Schema(description = "반려견 이름", example = "보리")
	private String name;

	@NotNull(message = "required_pet_age")
	@Min(value = 0, message = "invalid_pet_age_value")
	@Max(value = 50, message = "invalid_pet_age_value")
	@Pattern(regexp = "^[0-9]+$", message = "invalid_pet_age_format")
	@Schema(description = "나이", example = "2")
	private Integer age;

	@NotNull(message = "required_pet_gender")
	@Schema(description = "성별 (true=수컷, false=암컷)", example = "true")
	private Boolean gender;

	@NotBlank(message = "required_pet_breed")
	@Schema(description = "품종", example = "말티푸")
	private String breed;

	@NotNull(message = "required_pet_weight")
	@DecimalMin(value = "0.1", message = "invalid_pet_weight")
	@DecimalMax(value = "200.0", message = "invalid_pet_weight")
	@Schema(description = "몸무게", example = "4.3")
	private Double weight;
}
