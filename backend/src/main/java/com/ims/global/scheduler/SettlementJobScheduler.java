package com.ims.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job settlementJob;

    /** 매일 00:05 결산 배치 실행 */
    @Scheduled(cron = "0 5 0 * * *")
    public void runSettlementJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(settlementJob, params);
            log.info("결산 배치 실행 완료");
        } catch (Exception e) {
            log.error("결산 배치 실행 실패", e);
        }
    }
}
