package com.example.uc4monitor.batch;

import com.example.uc4monitor.service.Uc4SyncService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class Uc4MetadataSyncJobConfig {

    @Bean
    public Job uc4MetadataSyncJob(JobRepository jobRepository, Step uc4MetadataSyncStep) {
        return new JobBuilder("uc4MetadataSyncJob", jobRepository)
                .start(uc4MetadataSyncStep)
                .build();
    }

    @Bean
    public Step uc4MetadataSyncStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            Uc4SyncService syncService
    ) {
        return new StepBuilder("uc4MetadataSyncStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    syncService.sync();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
