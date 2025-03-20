package org.example.gangazido_be.gpt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.json.JSONObject;

@Service
public class WeatherService {

	@Value("${weather.api.key}") // application.yml에서 OpenWeather API 키 가져오기
	private String apiKey;

	private static final String WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather";
	private static final String AIR_POLLUTION_API_URL = "https://api.openweathermap.org/data/2.5/air_pollution"; // ✅ OpenWeather 공기질 API 사용

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
			responseJson.put("weather", weatherJson);
			responseJson.put("air_quality", airQualityJson); // ✅ OpenWeather API 사용

			return responseJson.toString(2); // JSON 예쁘게 포맷
		} catch (Exception e) {
			return "❌ 데이터 요청 중 오류 발생: " + e.getMessage();
		}
	}

	private JSONObject fetchWeatherData(double latitude, double longitude) {
		String url = UriComponentsBuilder.fromHttpUrl(WEATHER_API_URL)
			.queryParam("lat", latitude)
			.queryParam("lon", longitude)
			.queryParam("appid", apiKey) // 🔥 환경 변수에서 불러온 API 키 사용
			.queryParam("units", "metric") // 섭씨 온도 반환
			.toUriString();

		RestTemplate restTemplate = new RestTemplate();
		String response = restTemplate.getForObject(url, String.class);
		JSONObject json = new JSONObject(response);

		// ✅ 날씨 데이터 파싱
		JSONObject weatherJson = new JSONObject();
		weatherJson.put("condition", json.getJSONArray("weather").getJSONObject(0).getString("main")); // 날씨 상태
		weatherJson.put("temperature", json.getJSONObject("main").getDouble("temp")); // 온도

		return weatherJson;
	}

	private JSONObject fetchAirPollutionData(double latitude, double longitude) {
		String url = UriComponentsBuilder.fromHttpUrl(AIR_POLLUTION_API_URL)
			.queryParam("lat", latitude)
			.queryParam("lon", longitude)
			.queryParam("appid", apiKey) // 🔥 OpenWeather API 키 사용
			.toUriString();

		RestTemplate restTemplate = new RestTemplate();
		String response = restTemplate.getForObject(url, String.class);
		JSONObject json = new JSONObject(response);

		// ✅ OpenWeather API 응답에서 미세먼지 데이터 파싱
		JSONObject pollution = json.getJSONArray("list").getJSONObject(0).getJSONObject("components");

		JSONObject airQualityJson = new JSONObject();
		airQualityJson.put("pm10", pollution.getDouble("pm10"));   // 미세먼지 (PM10)
		airQualityJson.put("pm2_5", pollution.getDouble("pm2_5")); // 초미세먼지 (PM2.5)

		return airQualityJson;
	}
}
