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
		String htmlContent = "<div style='font-family: Arial, sans-serif; text-align: center;'>" +
			"<h2 style='color: #f59e0b;'>🌟 Gangazido 회원가입 인증 🌟</h2>" +
			"<p>아래 인증 코드를 입력해주세요.</p>" +
			"<div style='margin: 20px auto; padding: 10px 20px; background-color: #fef3c7; " +
			"border: 1px solid #fcd34d; border-radius: 8px; display: inline-block;'>" +
			"<strong style='font-size: 24px; color: #b45309; letter-spacing: 2px;'>" + code + "</strong>" +
			"</div>" +
			"<p style='color: #6b7280; font-size: 12px;'>해당 코드는 5분 동안만 유효합니다.</p>" +
			"<p style='font-size: 11px;'>Gangazido 팀 드림 🐶</p>" +
			"</div>";
		emailUtil.sendEmail(email, "이메일 인증 코드", htmlContent);
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
