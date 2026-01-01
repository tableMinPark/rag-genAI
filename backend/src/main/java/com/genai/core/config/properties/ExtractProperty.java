package com.genai.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "extract")
public class ExtractProperty {

    private String macSnfPath;

    private String windowSnfPath;

    private String linuxSnfPath;
}
