package com.example.uc4monitor.batch;

import com.example.uc4monitor.service.AnomalyDetectionService;
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
public class Uc4RuntimeAnomalyDetectionJobConfig {

    @Bean
    public Job uc4RuntimeAnomalyDetectionJob(JobRepository jobRepository, Step uc4RuntimeAnomalyDetectionStep) {
        return new JobBuilder("uc4RuntimeAnomalyDetectionJob", jobRepository)
                .start(uc4RuntimeAnomalyDetectionStep)
                .build();
    }

    @Bean
    public Step uc4RuntimeAnomalyDetectionStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            AnomalyDetectionService anomalyDetectionService,
            JobParameterSupport jobParameterSupport
    ) {
        return new StepBuilder("uc4RuntimeAnomalyDetectionStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    anomalyDetectionService.detectFor(jobParameterSupport.resolveBusinessDate(chunkContext));
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
