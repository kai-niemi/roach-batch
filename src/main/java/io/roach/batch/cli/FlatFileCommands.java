package io.roach.batch.cli;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.shell.ExitRequest;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.FileCopyUtils;

import io.roach.batch.config.DataSourceFactory;
import io.roach.batch.flatfile.FlatFileReaderBuilder;
import io.roach.batch.flatfile.FlatFileResourceWriterBuilder;
import io.roach.batch.flatfile.FlatFileStreamWriterBuilder;
import io.roach.batch.flatfile.JdbcBatchWriterBuilder;
import io.roach.batch.flatfile.JobScheduler;
import io.roach.batch.flatfile.schema.FlatFileSchema;
import io.roach.batch.flatfile.schema.FlatFileSchemaBuilder;
import io.roach.batch.io.ResourceResolver;

@ShellComponent
@ShellCommandGroup("Flat File Commands")
public class FlatFileCommands {
    @Autowired
    private JobScheduler jobScheduler;

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @ShellMethod(value = "Pipe a source file to a destination (TEST 0)")
    public void t0() throws IOException {
        flatPipe("classpath:samples/test_sm.txt", "(stdout)", false);
    }

    @ShellMethod(value = "Pipe a source file to a destination")
    public void flatPipe(
            @ShellOption(help = "input file") String inputFile,
            @ShellOption(help = "output file", defaultValue = "(stdout)") String outputFile,
            @ShellOption(help = "quit after completion", defaultValue = "false") boolean quit
    ) throws IOException {
        if (inputFile.equals(outputFile)) {
            throw new IllegalArgumentException("Source and target file must be different");
        }

        final OutputStream out;
        if ("(stdout)".equals(outputFile)) {
            out = new BufferedOutputStream(System.out, 8192 * 2) {
                @Override
                public void close() {
                }
            };
        } else {
            out = new BufferedOutputStream(Files.newOutputStream(Paths.get(outputFile)), 8291 * 2);
        }

        try (InputStream in = new BufferedInputStream(ResourceResolver.resolve(inputFile).getInputStream())) {
            FileCopyUtils.copy(in, out);
        } finally {
            out.close();
        }

        if (quit) {
            System.exit(0);
        }
    }

    @ShellMethod(value = "Convert a flat file to CSV (TEST 1)")
    public void t1() throws IOException {
        flatToCSV("classpath:samples/test_sm.txt", "classpath:samples/test.json", 0, "(stdout)", 256, false);
    }

    @ShellMethod(value = "Convert a flat file to CSV (TEST 2)")
    public void t2() throws IOException {
        flatToCSV("classpath:samples/test_sm.txt", "classpath:samples/test.json", 0, "test.csv", 256, false);
    }

    @ShellMethod(value = "Convert a flat file to CSV")
    public void flatToCSV(
            @ShellOption(help = "input file with fixed length columns") String inputFile,
            @ShellOption(help = "input file schema") String schemaFile,
            @ShellOption(help = "lines to skip from input", defaultValue = "0") int linesToSkip,
            @ShellOption(help = "output CSV file", defaultValue = "(stdout)") String outputFile,
            @ShellOption(help = "batch size (items to read before writing)", defaultValue = "256") int batchSize,
            @ShellOption(help = "quit after completion", defaultValue = "false") boolean quit
    ) throws IOException {
        if (inputFile.equals(outputFile)) {
            throw new IllegalArgumentException("Source and target file must be different");
        }

        final Resource inputResource = ResourceResolver.resolve(inputFile);

        final FlatFileSchema flatFileSchema = new FlatFileSchemaBuilder()
                .setSchemaPath(ResourceResolver.resolve(schemaFile))
                .build();

        final ItemReader<List<String>> itemReader = FlatFileReaderBuilder.instance()
                .setFlatFileSchema(flatFileSchema)
                .setInputResource(inputResource)
                .setLinesToSkip(linesToSkip)
                .build();

        final ItemWriter<List<String>> itemWriter;
        if ("(stdout)".equals(outputFile)) {
            itemWriter = FlatFileStreamWriterBuilder.instance()
                    .setFlatFileSchema(flatFileSchema)
                    .setOutputWriter(new OutputStreamWriter(System.out))
                    .build();
        } else {
            itemWriter = FlatFileResourceWriterBuilder.instance()
                    .setFlatFileSchema(flatFileSchema)
                    .setOutputResource(new FileSystemResource(outputFile))
                    .build();
        }

        jobScheduler.scheduleJob(inputResource, itemReader, itemWriter, batchSize);

        if (quit) {
            System.exit(0);
        }
    }

    @ShellMethod(value = "Convert a flat file to SQL inserts (TEST 3)")
    public void t3() throws IOException {
        flatToSQL("classpath:samples/test_sm.txt", "classpath:samples/test.json", 0, 256,
                "jdbc:postgresql://192.168.1.99:26300/roach_batch?sslmode=disable", "root", "", false);
    }

    @ShellMethod(value = "Convert a flat file to SQL inserts")
    public void flatToSQL(
            @ShellOption(help = "input file with fixed length columns") String inputFile,
            @ShellOption(help = "input file schema") String schemaFile,
            @ShellOption(help = "lines to skip", defaultValue = "0") int linesToSkip,
            @ShellOption(help = "batch size (items to read for each batch insert)", defaultValue = "512") int batchSize,
            @ShellOption(help = "target database JDBC url", defaultValue = "jdbc:postgresql://localhost:26257/roach_batch?sslmode=disable") String jdbcUrl,
            @ShellOption(help = "target database JDBC user name", defaultValue = "root") String jdbcUsername,
            @ShellOption(help = "target database JDBC password") String jdbcPassword,
            @ShellOption(help = "quit after completion", defaultValue = "false") boolean quit
    ) throws IOException {

        final FlatFileSchema flatFileSchema = new FlatFileSchemaBuilder()
                .setSchemaPath(ResourceResolver.resolve(schemaFile))
                .build();

        final Resource inputResource = ResourceResolver.resolve(inputFile);

        final ItemReader<List<String>> itemReader = FlatFileReaderBuilder.instance()
                .setFlatFileSchema(flatFileSchema)
                .setInputResource(inputResource)
                .setLinesToSkip(linesToSkip)
                .build();

        DataSourceProperties properties = new DataSourceProperties();
        properties.setDriverClassName("org.postgresql.Driver");
        properties.setUrl(jdbcUrl);
        properties.setUsername(jdbcUsername);
        properties.setPassword(jdbcPassword);

        ItemWriter<List<String>> itemWriter = JdbcBatchWriterBuilder.instance()
                .setProperties(properties)
                .setDataSourceFactory(dataSourceFactory)
                .setFlatFileSchema(flatFileSchema)
                .build();

        jobScheduler.scheduleJob(inputResource, itemReader, itemWriter, batchSize);

        if (quit) {
            System.exit(0);
        }
    }
}
