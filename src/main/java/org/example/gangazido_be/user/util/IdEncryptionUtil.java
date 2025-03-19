package org.example.gangazido_be.user.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class IdEncryptionUtil {

	@Value("${app.encryption.key}")  // 기본값 제거
	private String encryptionKey;

	@Value("${app.encryption.iv}")   // 기본값 제거
	private String encryptionIv;

	// 정수 ID를 암호화하여 문자열로 반환
	public String encrypt(Integer id) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
			IvParameterSpec ivSpec = new IvParameterSpec(encryptionIv.getBytes(StandardCharsets.UTF_8));

			cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
			byte[] encrypted = cipher.doFinal(id.toString().getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(encrypted);
		} catch (Exception e) {
			throw new RuntimeException("ID 암호화 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	// 암호화된 문자열을 복호화하여 정수 ID로 반환
	public Integer decrypt(String encryptedId) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
			IvParameterSpec ivSpec = new IvParameterSpec(encryptionIv.getBytes(StandardCharsets.UTF_8));

			cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
			byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedId));
			return Integer.parseInt(new String(decrypted, StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new RuntimeException("ID 복호화 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}
}
