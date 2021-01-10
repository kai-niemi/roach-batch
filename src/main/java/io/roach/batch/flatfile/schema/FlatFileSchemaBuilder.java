package io.roach.batch.flatfile.schema;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;

import org.springframework.core.io.Resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

public class FlatFileSchemaBuilder {
    private Resource schemaPath;

    public FlatFileSchemaBuilder setSchemaPath(Resource schemaPath) {
        this.schemaPath = schemaPath;
        return this;
    }

    private Gson createGson() {
        return new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE)
                .setPrettyPrinting()
                .create();
    }

    public FlatFileSchema build() throws IOException {
        try (InputStreamReader reader = new InputStreamReader(schemaPath.getInputStream())) {
            return createGson().fromJson(reader, FlatFileSchema.class);
        } catch (IllegalArgumentException | JsonParseException e) {
            throw new IOException("Error parsing schema", e);
        }
    }
}
