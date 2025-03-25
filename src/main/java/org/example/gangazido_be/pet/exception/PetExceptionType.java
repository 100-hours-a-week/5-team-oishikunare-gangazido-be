package org.example.gangazido_be.pet.exception;

import lombok.Getter;

@Getter
public enum PetExceptionType {

	INVALID_PET_NAME_FORMAT("invalid_pet_name_format"),
	INVALID_PET_NAME_LENGTH("invalid_pet_name_length"),
	REQUIRED_PET_NAME("required_pet_name"),

	REQUIRED_PET_AGE("required_pet_age"),
	INVALID_PET_AGE_VALUE("invalid_pet_age_value"),

	REQUIRED_PET_WEIGHT("required_pet_weight"),
	INVALID_PET_WEIGHT("invalid_pet_weight"),

	REQUIRED_PET_BREED("required_pet_breed"),

	REQUIRED_PET_GENDER("required_pet_gender"),

	REQUIRED_PERMISSION("required_permission"),
	ALREADY_EXISTS_PET("already_exists_pet"),

	NOT_FOUND_USER("not_found_user"),
	NOT_FOUND_PET("not_found_pet");

	private final String message;

	PetExceptionType(String message) {
		this.message = message;
	}
}
