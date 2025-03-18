//WeatherService
package org.example.gangazido_be.gpt.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.json.JSONObject;

@Service
public class WeatherService {
	private static final String API_KEY = "5c1a2dea8139784195e2f93082ac3b97"; // 🔥 실제 API 키 사용!
	private static final String WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather";
	private static final String AIR_POLLUTION_API_URL = "https://api.openweathermap.org/data/2.5/air_pollution";

	public String getWeather(double latitude, double longitude) {
		// ✅ 날씨 및 대기질 정보 가져오기
		JSONObject weatherJson = fetchWeatherData(latitude, longitude);
		JSONObject airQualityJson = fetchAirPollutionData(latitude, longitude);

		// ✅ JSON 형태로 응답 반환
		JSONObject responseJson = new JSONObject();
		responseJson.put("weather", weatherJson);
		responseJson.put("air_quality", airQualityJson);

		return responseJson.toString(2); // JSON 예쁘게 포맷
	}

	private JSONObject fetchWeatherData(double latitude, double longitude) {
		String url = UriComponentsBuilder.fromHttpUrl(WEATHER_API_URL)
			.queryParam("lat", latitude)
			.queryParam("lon", longitude)
			.queryParam("appid", API_KEY)
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
			.queryParam("appid", API_KEY)
			.toUriString();

		RestTemplate restTemplate = new RestTemplate();
		String response = restTemplate.getForObject(url, String.class);
		JSONObject json = new JSONObject(response);

		JSONObject pollution = json.getJSONArray("list").getJSONObject(0).getJSONObject("components");

		// ✅ 미세먼지 데이터 파싱
		JSONObject airQualityJson = new JSONObject();
		airQualityJson.put("pm10", pollution.getDouble("pm10"));   // 미세먼지 (PM10)
		airQualityJson.put("pm2_5", pollution.getDouble("pm2_5")); // 초미세먼지 (PM2.5)

		return airQualityJson;
	}
}
