package org.example.gangazido_be.llm.model;
// ✅ LlmRequest: LLM API 요청을 위한 데이터 모델 (클라이언트가 전송하는 JSON 요청을 매핑)
public class LlmRequest {
	private double latitude; // ✅ 사용자의 위도 값 (위치 기반 서비스)
	private double longitude; // ✅ 사용자의 경도 값
	private String message; // ✅ 사용자가 보낸 메시지 (GPT 입력)

	//public LlmRequest() {}  // 기본 생성자

	public LlmRequest(double latitude, double longitude, String message) { // ✅ 모든 필드를 초기화하는 생성자
		this.latitude = latitude;
		this.longitude = longitude;
		this.message = message;
	}
	// ✅ Getter / Setter 메서드 (필드 값 읽기 & 변경)
	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
