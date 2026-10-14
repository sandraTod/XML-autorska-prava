package com.example.xml_backend.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class A1WebConfiguration implements WebMvcConfigurer {
	
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (var converter : converters) {
            if (converter instanceof StringHttpMessageConverter strings) {
                strings.setDefaultCharset(StandardCharsets.UTF_8);
            }
        }
    }
}
