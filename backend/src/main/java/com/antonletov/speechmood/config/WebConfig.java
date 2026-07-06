package com.antonletov.speechmood.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.voice-dir}")
    private String voiceDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:" + Path.of(uploadDir).toAbsolutePath() + "/");
        registry.addResourceHandler("/uploads/voice/**")
                .addResourceLocations("file:" + Path.of(voiceDir).toAbsolutePath() + "/");
    }
}
