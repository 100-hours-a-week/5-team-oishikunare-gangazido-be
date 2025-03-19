package org.example.gangazido_be.gpt.controller;

import org.example.gangazido_be.gpt.service.WeatherService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")  // 모든 엔드포인트가 "/api" 하위에서 실행됨
public class WeatherController {
	private final WeatherService weatherService;

	public WeatherController(WeatherService weatherService) {
		this.weatherService = weatherService;
	}

	@GetMapping("/weather")
	public String getWeather(@RequestParam double lat, @RequestParam double lon) {
		return weatherService.getWeather(lat, lon);
	}
}
