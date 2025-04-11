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
	private final Map<String, Bucket> markerBuckets = new ConcurrentHashMap<>();
	private final Map<String, Bucket> emailAuthBuckets = new ConcurrentHashMap<>(); // ✅ 이메일 인증 추가
	private final Map<String, Bucket> chatBuckets = new ConcurrentHashMap<>();

	public Bucket getChatBucket(String ipAddress) {
		return chatBuckets.computeIfAbsent(ipAddress, ip -> createChatRateLimit());
	}

	// 로그인 버킷 가져오기 (1분당 100회 요청 제한)
	public Bucket getLoginBucket(String ipAddress) {
		return loginBuckets.computeIfAbsent(ipAddress, ip -> createLoginRateLimit());
	}

	// 회원가입 버킷 가져오기 (1시간당 50회 요청 제한)
	public Bucket getSignupBucket(String ipAddress) {
		return signupBuckets.computeIfAbsent(ipAddress, ip -> createSignupRateLimit());
	}

	// 중복 확인 버킷 가져오기 (1분당 300회 요청 제한)
	public Bucket getDuplicateCheckBucket(String ipAddress) {
		return duplicateCheckBuckets.computeIfAbsent(ipAddress, ip -> createDuplicateCheckRateLimit());
	}

	// 이미지 업로드 버킷 가져오기 (1시간당 200회 요청 제한)
	public Bucket getImageUploadBucket(String ipAddress) {
		return imageUploadBuckets.computeIfAbsent(ipAddress, ip -> createImageUploadRateLimit());
	}

	// 마커 등록 요청용 버킷 가져오기
	public Bucket getMarkerBucket(String ipAddress) {
		return markerBuckets.computeIfAbsent(ipAddress, ip -> createMarkerRateLimit());
	}

	// 이메일 인증 요청 제한 (10분에 5회 제한)
	public Bucket getEmailAuthBucket(String ipAddress) {
		return emailAuthBuckets.computeIfAbsent(ipAddress, ip -> createEmailAuthRateLimit());
	}


	// 로그인 요청 제한 (1분당 30회)
	private Bucket createLoginRateLimit() {
		Bandwidth limit = Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1)));
		return Bucket.builder()
			.addLimit(limit)
			.build();
	}

	// 회원가입 요청 제한 (1시간당 10회)
	private Bucket createSignupRateLimit() {
		Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofHours(1)));
		return Bucket.builder()
			.addLimit(limit)
			.build();
	}

	// 중복 확인 요청 제한 (1분당 100회)
	private Bucket createDuplicateCheckRateLimit() {
		Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
		return Bucket.builder()
			.addLimit(limit)
			.build();
	}

	// 이미지 업로드 요청 제한 (1시간당 40회)
	private Bucket createImageUploadRateLimit() {
		Bandwidth limit = Bandwidth.classic(30, Refill.intervally(30, Duration.ofHours(1)));
		return Bucket.builder()
			.addLimit(limit)
			.build();
	}

	// 마커 등록 요청 제한 (1분당 15회)
	private Bucket createMarkerRateLimit() {
		Bandwidth limit = Bandwidth.classic(15, Refill.intervally(15, Duration.ofMinutes(1)));
		return Bucket.builder()
			.addLimit(limit)
			.build();
	}

	// 이메일 인증 요청 제한 설정 (예: 10분에 5번)
	private Bucket createEmailAuthRateLimit() {
		Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(10)));
		return Bucket.builder().addLimit(limit).build();
	}

	private Bucket createChatRateLimit() {
		Bandwidth limit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1))); // 예: 1분에 20번
		return Bucket.builder()
			.addLimit(limit)
			.build();
	}

}
