package me._hanho.ultary.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiModerationResult {

	private boolean safe = true;
	private String category;
	private String reason;
}
