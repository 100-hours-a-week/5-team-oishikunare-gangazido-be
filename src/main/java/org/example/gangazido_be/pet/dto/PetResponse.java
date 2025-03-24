package org.example.gangazido_be.pet.dto;

import lombok.Builder;
import lombok.Getter;

import org.example.gangazido_be.pet.entity.Pet;

@Getter
@Builder
public class PetResponse {
	private String name;
	private String petProfileImage;
	private Integer age;
	private boolean gender;
	private String breed;
	private Double weight;

	public static PetResponse from(Pet pet) {
		return PetResponse.builder()
			.name(pet.getName())
			.petProfileImage(pet.getPetProfileImage())
			.age(pet.getAge())
			.gender(pet.getGender())
			.breed(pet.getBreed())
			.weight(pet.getWeight())
			.build();
	}
}
