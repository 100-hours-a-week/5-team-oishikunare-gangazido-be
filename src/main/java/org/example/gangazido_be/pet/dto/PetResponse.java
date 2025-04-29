package org.example.gangazido_be.pet.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.example.gangazido_be.pet.entity.Pet;
import org.example.gangazido_be.pet.exception.PetException;
import org.example.gangazido_be.pet.exception.PetExceptionType;
import org.springframework.http.HttpStatus;

@Setter
@Getter
@Builder
public class PetResponse {
	private String name;
	private String profileImage;
	private Integer age;
	private Boolean gender;
	private String breed;
	private Double weight;

	public static PetResponse from(Pet pet) {
		return PetResponse.builder()
			.name(pet.getName())
			.profileImage(pet.getProfileImage())
			.age(pet.getAge())
			.gender(pet.getGender())
			.breed(pet.getBreed())
			.weight(pet.getWeight())
			.build();
	}
}
