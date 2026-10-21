package me._hanho.ultary.security.bff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.Filter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"ai.moderation.worker-enabled=false", "security.bff.shared-key=integration-test-bff-key-not-for-production"})
class BffSecurityIntegrationTest {
	@Autowired WebApplicationContext context;
	@Autowired @Qualifier("springSecurityFilterChain") Filter security;
	@Autowired BffSecurityProperties bff;
	MockMvc mvc;

	@BeforeEach void fixture() { mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build(); }

	@Test void directCallToSocialLoginIsBlockedBeforeController() throws Exception {
		mvc.perform(post("/api/v1/auth/social/login").contentType("application/json").content("{}"))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("BFF_UNAUTHORIZED"));
	}
	@Test void keyDoesNotReplaceUserAuthentication() throws Exception {
		mvc.perform(get("/api/v1/auth/me").header("X-BFF-Key", bff.getSharedKey()))
				.andExpect(status().isUnauthorized());
	}
	@Test void permittedLoginStillRunsBodyValidationAfterKeyCheck() throws Exception {
		mvc.perform(post("/api/v1/auth/login").header("X-BFF-Key", bff.getSharedKey())
				.contentType("application/json").content("{}"))
				.andExpect(status().isBadRequest());
	}
}
