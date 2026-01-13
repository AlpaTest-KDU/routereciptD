package com.routerecipt.project.health;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
	
	@Value("${spring.profiles.active:unknown}")
	private String activeProfile;
	
	@GetMapping("/health")
	public Map<String, Object> health(){
		
		Map<String, Object> response = new HashMap<>();
		response.put("status", "up");
		response.put("profile", activeProfile);
		response.put("timestamp", LocalDateTime.now().toString());
		
		return response;
		
	}
	
}
