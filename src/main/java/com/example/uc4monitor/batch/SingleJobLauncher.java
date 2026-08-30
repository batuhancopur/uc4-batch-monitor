package com.example.uc4monitor.batch;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SingleJobLauncher implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final ApplicationArguments applicationArguments;
    private final Environment environment;
    private final Map<String, Job> jobsByName;

    public SingleJobLauncher(
            JobLauncher jobLauncher,
            ApplicationArguments applicationArguments,
            Environment environment,
            List<Job> jobs
    ) {
        this.jobLauncher = jobLauncher;
        this.applicationArguments = applicationArguments;
        this.environment = environment;
        this.jobsByName = jobs.stream().collect(Collectors.toMap(Job::getName, Function.identity()));
    }

    @Override
    public void run(String... args) throws Exception {
        String jobName = resolveJobName();
        Job job = jobsByName.get(jobName);
        if (job == null) {
            throw new IllegalArgumentException("Unknown jobName=%s. Available jobs: %s".formatted(jobName, jobsByName.keySet()));
        }

        JobExecution execution = jobLauncher.run(job, buildJobParameters());
        if (execution.getStatus() != BatchStatus.COMPLETED) {
            throw new IllegalStateException("Job %s finished with status %s".formatted(jobName, execution.getStatus()));
        }
    }

    private String resolveJobName() {
        String jobName = firstText(
                optionValue("jobName"),
                environment.getProperty("jobName"),
                environment.getProperty("JOB_NAME"),
                environment.getProperty("spring.batch.job.name")
        );
        if (!StringUtils.hasText(jobName)) {
            throw new IllegalArgumentException("Missing required --jobName parameter. Available jobs: " + jobsByName.keySet());
        }
        return jobName;
    }

    private JobParameters buildJobParameters() {
        return new JobParametersBuilder()
                .addString("launchTimestamp", Instant.now().toString())
                .toJobParameters();
    }

    private String optionValue(String optionName) {
        List<String> values = applicationArguments.getOptionValues(optionName);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(values.size() - 1);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
