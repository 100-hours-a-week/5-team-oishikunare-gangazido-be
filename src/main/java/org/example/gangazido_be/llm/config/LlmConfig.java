//llmconfig
//  LlmConfig: LLM(OpenAI 등) 관련 설정을 application.yml에서 불러오는 설정 클래스
package org.example.gangazido_be.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties; // yml 속성 매핑용 어노테이션
import org.springframework.context.annotation.Configuration; // 해당 클래스가 설정 클래스임을 명시

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Configuration // Spring이 설정 클래스(@Bean 등을 등록할 수 있음)로 인식하게 함
@ConfigurationProperties(prefix = "llm") //  application.yml에서 "llm."으로 시작하는 설정을 읽음
public class LlmConfig {
	//  Getter/Setter 메서드 (스프링이 값을 주입할 때 사용)
	private String apiKey; //  LLM API 키
	private String model; //  사용할 모델 이름 (예: gpt-3.5-turbo)

}

