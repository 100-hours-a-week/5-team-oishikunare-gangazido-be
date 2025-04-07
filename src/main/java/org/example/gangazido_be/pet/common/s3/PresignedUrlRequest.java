package org.example.gangazido_be.pet.common.s3;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PresignedUrlRequest {
	private String fileExtension;
	private String contentType;
}
