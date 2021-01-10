package io.roach.batch.io;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

public abstract class ResourceResolver {
    public static Resource resolve(String resource) throws IOException {
        return resolve(resource, Collections.emptyMap());
    }

    public static Resource resolve(String resource, Map<String, String> allParams) throws IOException {
        if (resource.startsWith("classpath:")) {
            return new ClassPathResource(resource.substring("classpath:".length()));
        }
        if (resource.startsWith("file:")) {
            return new FileSystemResource(resource);
        }
        if (resource.startsWith("http:")) {
            return new UrlResource(resource);
        }
        if (resource.startsWith("s3:")) {
            return new S3BucketResource(resource, allParams);
        }
        throw new IllegalArgumentException("No resource matching scheme: " + resource);
    }

}
