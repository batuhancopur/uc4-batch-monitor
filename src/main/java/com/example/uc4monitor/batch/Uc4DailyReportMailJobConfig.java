package com.example.uc4monitor.batch;

import com.example.uc4monitor.mail.DailyReportMailService;
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
public class Uc4DailyReportMailJobConfig {

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
            DailyReportMailService dailyReportMailService,
            JobParameterSupport jobParameterSupport
    ) {
        return new StepBuilder("uc4DailyReportMailStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    dailyReportMailService.sendDailyReport(jobParameterSupport.resolveBusinessDate(chunkContext));
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
