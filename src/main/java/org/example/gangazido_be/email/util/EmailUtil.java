package org.example.gangazido_be.email.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailUtil {

	private final JavaMailSender mailSender;

	public void sendEmail(String to, String subject, String htmlContent) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlContent, true); // ✅ true → HTML 사용
			mailSender.send(message);
		} catch (MessagingException e) {
			throw new RuntimeException("이메일 전송 실패", e);
		}
	}
}
