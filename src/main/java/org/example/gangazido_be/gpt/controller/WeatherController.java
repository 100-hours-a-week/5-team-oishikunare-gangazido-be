package org.example.gangazido_be.gpt.controller;

import org.example.gangazido_be.gpt.service.WeatherService;
import org.springframework.web.bind.annotation.*;

@RestController //레스트 컨트롤러
@RequestMapping("/api")  // 모든 엔드포인트가 "/api" 하위에서 실행됨
public class WeatherController { //  WeatherService 객체 (의존성 주입)
	private final WeatherService weatherService;
	//  생성자를 통해 WeatherService 주입 (Spring이 자동으로 주입)
	public WeatherController(WeatherService weatherService) {
		this.weatherService = weatherService;
	}

	//  날씨 및 대기질 정보 가져오기 (위도, 경도 기반)
	@GetMapping("/weather") //  HTTP GET 요청을 "/api/weather"로 매핑
	public String getWeather(@RequestParam double lat, @RequestParam double lon) {
		//  위도(lat), 경도(lon)를 받아서 WeatherService의 getWeather() 호출
		return weatherService.getWeather(lat, lon); // OpenWeather API 사용
	}
}
