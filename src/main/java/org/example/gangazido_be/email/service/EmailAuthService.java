package org.example.gangazido_be.email.service;

import org.example.gangazido_be.email.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailAuthService {

	private final EmailUtil emailUtil;
	private final RedisTemplate<String, String> redisTemplate;
	private static final long EXPIRE_TIME = 60 * 5; // 5분

	public void sendAuthCode(String email) {
		String code = generateCode();
		redisTemplate.opsForValue().set("email:" + email, code, EXPIRE_TIME, TimeUnit.SECONDS);
		emailUtil.sendEmail(email, "이메일 인증 코드", "인증 코드는: " + code);
	}


	public boolean verifyCode(String email, String code) {
		String key = "email:" + email;
		String savedCode = redisTemplate.opsForValue().get(key);
		return code.equals(savedCode);
	}

	private String generateCode() {
		return String.valueOf((int)(Math.random() * 900000) + 100000); // 6자리 숫자
	}
}
