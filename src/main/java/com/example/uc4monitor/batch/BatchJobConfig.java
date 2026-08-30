package com.example.uc4monitor.batch;

import com.example.uc4monitor.mail.DailyReportMailService;
import com.example.uc4monitor.service.AnomalyDetectionService;
import com.example.uc4monitor.service.Uc4SyncService;
import java.time.LocalDate;
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
public class BatchJobConfig {

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
            AnomalyDetectionService anomalyDetectionService
    ) {
        return new StepBuilder("uc4RuntimeAnomalyDetectionStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    anomalyDetectionService.detectFor(resolveBusinessDate(chunkContext));
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Job uc4DailyReportMailJob(JobRepository jobRepository, Step uc4DailyReportMailStep) {
        return new JobBuilder("uc4DailyReportMailJob", jobRepository)
                .start(uc4DailyReportMailStep)
                .build();
    }

    @Bean
    public Step uc4DailyReportMailStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DailyReportMailService dailyReportMailService
    ) {
        return new StepBuilder("uc4DailyReportMailStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    dailyReportMailService.sendDailyReport(resolveBusinessDate(chunkContext));
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    private LocalDate resolveBusinessDate(org.springframework.batch.core.scope.context.ChunkContext chunkContext) {
        Object value = chunkContext.getStepContext().getJobParameters().get("businessDate");
        if (value == null || value.toString().isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(value.toString());
    }
}
