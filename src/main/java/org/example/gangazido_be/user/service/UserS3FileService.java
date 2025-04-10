package org.example.gangazido_be.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserS3FileService {

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final Logger logger = LoggerFactory.getLogger(UserS3FileService.class);

	@Value("${aws.s3.bucket}")
	private String bucketName;

	@Value("${aws.region}")
	private String region;

	@Value("${aws.s3.presigned-url.expiration:600}")
	private long presignedUrlExpiration; // 초 단위, 기본값 10분

	@Autowired
	public UserS3FileService(S3Client s3Client, S3Presigner s3Presigner) {
		this.s3Client = s3Client;
		this.s3Presigner = s3Presigner;
	}

	/**
	 * 프로필 이미지 업로드를 위한 Presigned URL 생성
	 *
	 * @param fileExtension 파일 확장자
	 * @param contentType 파일 MIME 타입
	 * @return Map: presignedUrl과 fileKey 포함
	 */
	public Map<String, String> generatePresignedUrlForProfileImage(String fileExtension, String contentType) {
		String key = "user/" + UUID.randomUUID().toString() + fileExtension;

		// 메타데이터 추가하지 않음 (또는 필요한 경우만 명시적으로 추가)
		PutObjectRequest objectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(key)
			.contentType(contentType)
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofSeconds(presignedUrlExpiration))
			.putObjectRequest(objectRequest)
			.build();

		PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

		Map<String, String> result = new HashMap<>();
		result.put("presignedUrl", presignedRequest.url().toString());
		result.put("fileKey", key);
		result.put("s3Url", getS3Url(key));

		return result;
	}

	/**
	 * 파일 삭제
	 *
	 * @param fileKey S3 객체 키
	 * @return 삭제 성공 여부
	 */
	public boolean deleteFile(String fileKey) {
		if (fileKey == null || fileKey.isEmpty()) {
			return false;
		}

		try {
			// S3에서 객체키만 추출 (URL이 전달된 경우 처리)
			if (fileKey.startsWith("http")) {
				fileKey = extractKeyFromUrl(fileKey);
			}

			DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
				.bucket(bucketName)
				.key(fileKey)
				.build();

			s3Client.deleteObject(deleteRequest);
			logger.info("S3 파일 삭제 완료: {}", fileKey);
			return true;
		} catch (Exception e) {
			logger.error("S3 파일 삭제 실패: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * S3 URL에서 객체 키 추출
	 *
	 * @param url S3 URL
	 * @return 객체 키
	 */
	private String extractKeyFromUrl(String url) {
		// 예시: https://bucket-name.s3.region.amazonaws.com/profiles/uuid.jpg
		String[] parts = url.split("/");
		StringBuilder keyBuilder = new StringBuilder();

		// 도메인 이후의 모든 경로를 키로 사용
		boolean foundDomain = false;
		for (String part : parts) {
			if (foundDomain) {
				if (keyBuilder.length() > 0) {
					keyBuilder.append("/");
				}
				keyBuilder.append(part);
			} else if (part.contains(".amazonaws.com")) {
				foundDomain = true;
			}
		}

		return keyBuilder.toString();
	}

	/**
	 * S3 URL 생성
	 *
	 * @param key S3 객체 키
	 * @return S3 URL
	 */
	public String getS3Url(String key) {
		return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
	}

	/**
	 * 객체가 존재하는지 확인
	 *
	 * @param key S3 객체 키
	 * @return 존재 여부
	 */
	public boolean doesObjectExist(String key) {
		try {
			HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
				.bucket(bucketName)
				.key(key)
				.build();

			s3Client.headObject(headObjectRequest);
			return true;
		} catch (NoSuchKeyException e) {
			return false;
		} catch (Exception e) {
			logger.error("S3 객체 존재 확인 실패: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * 바이트 배열을 S3에 직접 업로드 (마이그레이션 등의 목적으로 사용)
	 * S3 저장 경로를 "uploads/user"로 변경
	 *
	 * @param fileContent 파일 내용 바이트 배열
	 * @param fileKey S3 객체 키 (null이면 자동 생성)
	 * @param contentType 파일 MIME 타입
	 * @return S3 URL
	 */
	// public String uploadFile(byte[] fileContent, String fileKey, String contentType) {
	// 	try {
	// 		// fileKey가 null이면 자동 생성 (경로 변경)
	// 		if (fileKey == null || fileKey.isEmpty()) {
	// 			String extension = contentType != null ?
	// 				"." + contentType.substring(contentType.lastIndexOf("/") + 1) : ".jpg";
	// 			fileKey = "user/" + UUID.randomUUID().toString() + extension;
	// 		}
	//
	// 		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
	// 			.bucket(bucketName)
	// 			.key(fileKey)
	// 			.contentType(contentType)
	// 			.build();
	//
	// 		s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileContent));
	// 		logger.info("S3에 파일 업로드 완료: {}", fileKey);
	//
	// 		return getS3Url(fileKey);
	// 	} catch (Exception e) {
	// 		logger.error("S3 파일 업로드 실패: {}", e.getMessage());
	// 		throw new RuntimeException("S3 파일 업로드 실패: " + e.getMessage());
	// 	}
	// }
}
