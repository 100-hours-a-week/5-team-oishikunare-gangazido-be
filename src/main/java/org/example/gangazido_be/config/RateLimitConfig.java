package org.example.gangazido_be.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

	// IP별 버킷을 저장하는 맵
	private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
	private final Map<String, Bucket> signupBuckets = new ConcurrentHashMap<>();
	private final Map<String, Bucket> duplicateCheckBuckets = new ConcurrentHashMap<>();
	private final Map<String, Bucket> imageUploadBuckets = new ConcurrentHashMap<>();

	// 로그인 버킷 가져오기 (1분당 10회 요청 제한)
	public Bucket getLoginBucket(String ipAddress) {
		return loginBuckets.computeIfAbsent(ipAddress, ip -> createLoginRateLimit());
	}

	// 회원가입 버킷 가져오기 (1시간당 5회 요청 제한)
	public Bucket getSignupBucket(String ipAddress) {
		return signupBuckets.computeIfAbsent(ipAddress, ip -> createSignupRateLimit());
	}

	// 중복 확인 버킷 가져오기 (1분당 30회 요청 제한)
	public Bucket getDuplicateCheckBucket(String ipAddress) {
		return duplicateCheckBuckets.computeIfAbsent(ipAddress, ip -> createDuplicateCheckRateLimit());
	}

	// 이미지 업로드 버킷 가져오기 (1시간당 20회 요청 제한)
	public Bucket getImageUploadBucket(String ipAddress) {
		return imageUploadBuckets.computeIfAbsent(ipAddress, ip -> createImageUploadRateLimit());
	}

	// 로그인 요청 제한 (1분당 10회)
	private Bucket createLoginRateLimit() {
		Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
		return Bucket.builder()
			.addLimit(limit)
			.build();
	}

	// 회원가입 요청 제한 (1시간당 5회)
	private Bucket createSignupRateLimit() {
		Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofHours(1)));
		return Bucket.builder()
			.addLimit(limit)
			.build();
	}

	// 중복 확인 요청 제한 (1분당 30회)
	private Bucket createDuplicateCheckRateLimit() {
		Bandwidth limit = Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1)));
		return Bucket.builder()
			.addLimit(limit)
			.build();
	}

	// 이미지 업로드 요청 제한 (1시간당 20회)
	private Bucket createImageUploadRateLimit() {
		Bandwidth limit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofHours(1)));
		return Bucket.builder()
			.addLimit(limit)
			.build();
	}
}
