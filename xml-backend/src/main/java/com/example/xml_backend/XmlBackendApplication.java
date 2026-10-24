package com.example.xml_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class XmlBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(XmlBackendApplication.class, args);
	}

}
