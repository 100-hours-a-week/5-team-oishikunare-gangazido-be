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
	private boolean gender;
	private String breed;
	private Double weight;

	public static PetResponse from(Pet pet) {
		if (pet.getName() == null || pet.getAge() == null || pet.getGender() == null || pet.getBreed() == null || pet.getWeight() == null) {
			throw new PetException(HttpStatus.NOT_FOUND, PetExceptionType.NOT_FOUND_PET.getMessage());
		}

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
