//✅ WeatherService: OpenWeather API를 이용해 날씨 및 대기질 데이터를 가져오는 서비스

package org.example.gangazido_be.gpt.service;

import org.springframework.beans.factory.annotation.Value; // ✅ application.yml에서 환경 변수 값 가져오기
import org.springframework.stereotype.Service; // ✅ HTTP 요청을 위한 RestTemplate 사용
import org.springframework.web.client.RestTemplate; // ✅ URL을 쉽게 조립하기 위한 유틸
import org.json.JSONObject; // ✅ JSON 데이터 처리 라이브러리
import org.springframework.web.util.UriComponentsBuilder;
import org.json.JSONObject;

@Service
public class WeatherService { // ✅ 이 클래스가 서비스 계층의 Bean으로 등록되도록 지정 (Spring이 관리)

	@Value("${weather.api.key}") // application.yml에서 OpenWeather API 키 가져오기
	private String apiKey; // ✅ application.yml에 저장된 OpenWeather API 키 주입
	// ✅ OpenWeather 날씨 API 및 공기질 API 엔드포인트
	private static final String WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather";
	private static final String AIR_POLLUTION_API_URL = "https://api.openweathermap.org/data/2.5/air_pollution"; // ✅ OpenWeather 공기질 API 사용
	/**
	 * ✅ 위도(latitude)와 경도(longitude)를 기반으로 날씨 및 공기질 정보를 가져옴
	 */
	public String getWeather(double latitude, double longitude) {
		if (apiKey == null || apiKey.isEmpty()) {
			return "❌ API 키가 설정되지 않았습니다. 환경 변수를 확인하세요.";
		}

		try {
			// ✅ 날씨 및 대기질 정보 가져오기
			JSONObject weatherJson = fetchWeatherData(latitude, longitude);
			JSONObject airQualityJson = fetchAirPollutionData(latitude, longitude); // ✅ OpenWeather API로 대기질 정보 가져오기

			// ✅ JSON 형태로 응답 반환
			JSONObject responseJson = new JSONObject();
			responseJson.put("weather", weatherJson); // 날씨 데이터 포함
			responseJson.put("air_quality", airQualityJson); // 대기질 데이터 포함

			return responseJson.toString(2); // JSON 예쁘게 포맷하여 반환
		} catch (Exception e) {
			return "❌ 데이터 요청 중 오류 발생: " + e.getMessage();
		}
	}

	/**
	 * ✅ OpenWeather API에서 날씨 데이터를 가져오는 메서드
	 */

	private JSONObject fetchWeatherData(double latitude, double longitude) {
		// ✅ OpenWeather 날씨 API 요청 URL 생성
		String url = UriComponentsBuilder.fromHttpUrl(WEATHER_API_URL)
			.queryParam("lat", latitude)
			.queryParam("lon", longitude)
			.queryParam("appid", apiKey) // 🔥 환경 변수에서 불러온 API 키 사용
			.queryParam("units", "metric") // ✅ 섭씨 온도로 데이터 받기
			.toUriString();

		// ✅ RestTemplate을 이용해 HTTP 요청 수행
		RestTemplate restTemplate = new RestTemplate();
		String response = restTemplate.getForObject(url, String.class); // API 응답을 문자열(JSON)로 받음
		JSONObject json = new JSONObject(response); // JSON 객체로 변환

		// ✅ JSON 데이터에서 필요한 정보만 추출
		JSONObject weatherJson = new JSONObject();
		weatherJson.put("condition", json.getJSONArray("weather").getJSONObject(0).getString("main")); // 날씨 상태
		weatherJson.put("temperature", json.getJSONObject("main").getDouble("temp")); // 온도

		return weatherJson;
	}

	/**
	 * ✅ OpenWeather API에서 공기질 데이터를 가져오는 메서드
	 */

	private JSONObject fetchAirPollutionData(double latitude, double longitude) {
		// ✅ OpenWeather 공기질 API 요청 URL 생성
		String url = UriComponentsBuilder.fromHttpUrl(AIR_POLLUTION_API_URL)
			.queryParam("lat", latitude)
			.queryParam("lon", longitude)
			.queryParam("appid", apiKey) // 🔥 OpenWeather API 키 사용
			.toUriString();

		// ✅ RestTemplate을 이용해 HTTP 요청 수행
		RestTemplate restTemplate = new RestTemplate();
		String response = restTemplate.getForObject(url, String.class); // API 응답을 문자열(JSON)로 받음

		JSONObject json = new JSONObject(response); // JSON 객체로 변환

		// ✅ JSON 데이터에서 미세먼지 관련 정보 추출
		JSONObject pollution = json.getJSONArray("list").getJSONObject(0).getJSONObject("components");

		JSONObject airQualityJson = new JSONObject();
		airQualityJson.put("pm10", pollution.getDouble("pm10"));   // 미세먼지 (PM10)
		airQualityJson.put("pm2_5", pollution.getDouble("pm2_5")); // 초미세먼지 (PM2.5)
		System.out.println("[DEBUG] 대기질 원본 응답: " + response);  // WeatherService 내에서!


		return airQualityJson;
	}
}
