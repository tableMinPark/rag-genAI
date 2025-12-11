package com.genai.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "embed")
public class EmbedProperty {

    private String fileStorePath;

    private String tempDir;

    private int tokenSize;

    private int overlapSize;
}
