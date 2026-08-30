package com.example.uc4monitor.repository;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

@Component
public class SqlResourceLoader {

    private final ResourceLoader resourceLoader;

    public SqlResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String read(String location) {
        Resource resource = resourceLoader.getResource(location);
        try (var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read SQL resource: " + location, ex);
        }
    }
}
