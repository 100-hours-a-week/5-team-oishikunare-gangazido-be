package org.example.gangazido_be.pet.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PetCreateRequest {
	@NotBlank(message = "required_pet_name")
	@Size(max = 10, message = "invalid_pet_name_length")
	private String name;

	@NotNull(message = "required_pet_age")
	@Min(value = 0, message = "invalid_pet_age_value")
	@Max(value = 50, message = "invalid_pet_age_value")
	@Pattern(regexp = "^[0-9]+$", message = "invalid_pet_age_format")
	private Integer age;

	@NotNull(message = "required_pet_gender")
	private Integer gender;

	@NotBlank(message = "required_pet_breed")
	private String breed;

	@NotNull(message = "required_pet_weight")
	@DecimalMin(value = "0.1", message = "invalid_pet_weight")
	@DecimalMax(value = "200.0", message = "invalid_pet_weight")
	private Double weight;
}
