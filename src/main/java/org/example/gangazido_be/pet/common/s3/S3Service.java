package org.example.gangazido_be.pet.common.s3;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

	@Value("${aws.accessKey}")
	private String accessKey;

	@Value("${aws.secretKey}")
	private String secretKey;

	@Value("${aws.region}")
	private String region;

	@Value("${aws.s3.bucket}")
	private String bucket;

	@Value("${aws.s3.presigned-url.expiration}")
	private long expirationSeconds;

	public PresignedUrlResponse generatePresignedUrl(String fileExtension, String contentType) {
		String fileKey = "pet/" + UUID.randomUUID() + fileExtension;

		// presigner 설정
		AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
		S3Presigner presigner = S3Presigner.builder()
			.region(Region.of(region))
			.credentialsProvider(StaticCredentialsProvider.create(credentials))
			.build();

		PutObjectRequest objectRequest = PutObjectRequest.builder()
			.bucket(bucket)
			.key(fileKey)
			.contentType(contentType)
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofSeconds(expirationSeconds))
			.putObjectRequest(objectRequest)
			.build();

		PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

		return new PresignedUrlResponse(fileKey, presignedRequest.url().toString());
	}
}
