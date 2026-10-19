package me._hanho.ultary.domain.ai.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiHashtagResult {

	private List<String> hashtags = new ArrayList<>();
}
