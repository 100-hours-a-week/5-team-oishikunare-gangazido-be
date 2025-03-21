//llmresponse
// ✅ LlmResponse: LLM API 응답을 클라이언트에게 반환하는 데이터 모델
package org.example.gangazido_be.llm.model;

public class LlmResponse {
	private String message; // ✅ 응답 상태 메시지 (예: "success", "error code" 등)
	private String response; // ✅ GPT가 생성한 실제 응답 텍스트
	// ✅ 모든 필드를 초기화하는 생성자
	public LlmResponse(String message, String response) {
		this.message = message;
		this.response = response;
	}
	// ✅ Getter 메서드 (클라이언트가 응답 값을 읽을 수 있도록 제공)
	public String getMessage() {
		return message;
	}

	public String getResponse() {
		return response;
	}
}

