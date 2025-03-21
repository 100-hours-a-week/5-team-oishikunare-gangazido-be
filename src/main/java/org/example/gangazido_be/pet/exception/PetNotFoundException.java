package org.example.gangazido_be.pet.exception;

public class PetNotFoundException extends RuntimeException {
	public PetNotFoundException() {
		super(PetExceptionType.NOT_FOUND_PET.getMessage());
	}
}
