package io.roach.batch.web;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.roach.batch.config.ProxyProfileEnabled;
import io.roach.batch.flatfile.JobScheduler;
import io.roach.batch.flatfile.FlatFileReaderBuilder;
import io.roach.batch.flatfile.FlatFileStreamWriterBuilder;
import io.roach.batch.flatfile.schema.FlatFileSchema;
import io.roach.batch.flatfile.schema.FlatFileSchemaBuilder;
import io.roach.batch.io.ResourceResolver;

@RestController
@RequestMapping("/download")
@ProxyProfileEnabled
public class DownloadController implements ApplicationContextAware {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private ApplicationContext applicationContext;

    @Autowired
    private JobScheduler jobScheduler;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @GetMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadResource(@RequestParam Map<String, String> allParams)
            throws IOException {
        final String source = allParams.get("source");

        if (source == null) {
            throw new IllegalArgumentException("Missing required param [source]");
        }

        final Resource inputResource = ResourceResolver.resolve(source, allParams);

        final String schema = allParams.get("schema");

        final StreamingResponseBody responseBody;
        if (schema != null) {
            final int batchSize = Integer.parseInt(allParams.getOrDefault("batchSize", "256"));
            final int linesToSkip = Integer.parseInt(allParams.getOrDefault("linesToSkip", "0"));
            final Resource schemaResource = ResourceResolver.resolve(allParams.get("schema"), allParams);

            logger.debug("Processing [{}] with schema [{}] using [{}]. Skip [{}] lines. Batch size [{}]",
                    source, schema, inputResource, linesToSkip, batchSize);

            responseBody = copyAndConvertInputStream(inputResource, schemaResource, linesToSkip, batchSize);
        } else {
            logger.debug("Processing [{}] without schema using [{}]", source, schema);

            responseBody = copyInputStream(inputResource);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + source)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(responseBody);
    }

    public StreamingResponseBody copyInputStream(Resource input) {
        return outputStream -> {
            try (InputStream in = input.getInputStream()) {
                FileCopyUtils.copy(in, new BufferedOutputStream(outputStream));
            }
        };
    }

    public StreamingResponseBody copyAndConvertInputStream(Resource inputResource, Resource schemaResource,
                                                           int linesToSkip, int batchSize) {
        return outputStream -> {
            final FlatFileSchema flatFileSchema = new FlatFileSchemaBuilder()
                    .setSchemaPath(schemaResource)
                    .build();

            final ItemReader<List<String>> itemReader = FlatFileReaderBuilder.instance()
                    .setFlatFileSchema(flatFileSchema)
                    .setInputResource(inputResource)
                    .setLinesToSkip(linesToSkip)
                    .build();

            final ItemWriter<List<String>> itemWriter = FlatFileStreamWriterBuilder.instance()
                    .setFlatFileSchema(flatFileSchema)
                    .setOutputWriter(new OutputStreamWriter(new BufferedOutputStream(outputStream)))
                    .build();

            jobScheduler.scheduleJob(inputResource, itemReader, itemWriter, batchSize);
        };
    }
}
