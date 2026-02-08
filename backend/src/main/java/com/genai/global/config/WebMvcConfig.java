package com.genai.global.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final OctetStreamReadMsgConverter octetStreamReadMsgConverter;

    @Autowired
    public WebMvcConfig(OctetStreamReadMsgConverter octetStreamReadMsgConverter) {
        this.octetStreamReadMsgConverter = octetStreamReadMsgConverter;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(octetStreamReadMsgConverter);
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", c -> {
            return c.getPackageName().startsWith("com.genai.app") || c.getPackageName().startsWith("com.genai.core");
        });
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("*");
    }
}