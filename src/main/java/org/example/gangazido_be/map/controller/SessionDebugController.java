// 제리추가 api로 확인하면 콘솔에 세션 정보 뜸 (로그인 필수)

package org.example.gangazido_be.map.controller;

import java.util.Enumeration;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/debug")
public class SessionDebugController {

	private final HttpSession session;

	public SessionDebugController(HttpSession session) {
		this.session = session;
	}

	@GetMapping("/session-info")
	public void printSessionAttributes(HttpSession session) {
		System.out.println("=== 세션 정보 출력 ===");
		System.out.println("세션 ID: " + session.getId());
		System.out.println("세션 생성 시간: " + session.getCreationTime());
		System.out.println("세션 마지막 접근 시간: " + session.getLastAccessedTime());
		System.out.println("세션 최대 유효 시간: " + session.getMaxInactiveInterval() + "초");
		System.out.println("====================");

		Enumeration<String> attributeNames = session.getAttributeNames();
		while (attributeNames.hasMoreElements()) {
			String attributeName = attributeNames.nextElement();
			Object attributeValue = session.getAttribute(attributeName);
			System.out.println("세션 속성: " + attributeName + " = " + attributeValue);
		}
	}
}
