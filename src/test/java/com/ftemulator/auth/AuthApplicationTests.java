/*
 * ftemulator - ftemulator is a high-performance stock market investment simulator designed with extreme technical efficiency
 * 
 * Copyright (C) 2025-2025 Álex Frías (alexwebdev05)
 * Licensed under GNU Affero General Public License v3.0
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * For commercial licensing inquiries, please contact: alexwebdev05@proton.me
 * GitHub: https://github.com/alexwebdev05
 */
package com.ftemulator.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = AuthApplicationTests.Initializer.class)
class AuthApplicationTests {

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			// Set system properties before Spring context loads
			System.setProperty("JWT_SECRET", "test-jwt-secret-key-for-testing-purposes-only-do-not-use-in-production-environment");
			System.setProperty("REDIS_HOST", "localhost");
			System.setProperty("REDIS_PORT", "6379");
			System.setProperty("REDIS_PASSWORD", "redis");
		}
	}

	@Test
	void contextLoads() {
	}

}
