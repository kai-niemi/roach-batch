package io.roach.batch.flatfile;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class JobScheduler {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected JobLauncher jobLauncher;

    @Autowired
    protected JobBuilderFactory jobBuilderFactory;

    @Autowired
    protected StepBuilderFactory stepBuilderFactory;

    public void scheduleJob(Resource inputResource,
                            ItemReader<List<String>> itemReader,
                            ItemWriter<List<String>> itemWriter,
                            int batchSize) throws IOException {
        Step step = stepBuilderFactory.get("processingStep")
                .<List<String>, List<String>>chunk(batchSize)
                .reader(itemReader)
                .listener(new LoggingReadListener<>(inputResource, false))
                .processor(new PassThroughItemProcessor<>())
                .writer(itemWriter)
                .listener(new LoggingWriteListener<>())
                .build();

        Job job = jobBuilderFactory.get("importJob")
                .incrementer(new RunIdIncrementer())
                .flow(step)
                .end()
                .build();

        JobParameters params = new JobParametersBuilder()
                .addString("JobID", String.valueOf(System.currentTimeMillis()))
                .toJobParameters();
        try {
            jobLauncher.run(job, params);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            logger.error("Job execution error: %s", e);
        }
    }
}
