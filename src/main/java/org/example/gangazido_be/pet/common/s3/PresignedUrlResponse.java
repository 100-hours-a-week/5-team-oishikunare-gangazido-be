package org.example.gangazido_be.pet.common.s3;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PresignedUrlResponse {
	private String fileKey;
	private String presignedUrl;
	private String s3Url;
}
