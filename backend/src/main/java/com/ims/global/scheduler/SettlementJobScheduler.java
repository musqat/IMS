package com.ims.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementJobScheduler {

    /**
     * 컨테이너 기본 타임존에 의존하지 않는다.
     * 배포 환경(Render)은 UTC라 zone을 지정하지 않으면 09:05 KST에 실행된다.
     */
    static final ZoneId KST = ZoneId.of("Asia/Seoul");

    static final String PARAM_RUN_DATE = "runDate";

    private final JobLauncher jobLauncher;
    private final Job settlementJob;

    /**
     * 매일 00:05(KST) 결산 배치 실행
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void runSettlementJob() {
        runFor(LocalDate.now(KST));
    }

    /**
     * 지정한 실행일로 결산 Job을 기동한다.
     * - JobParameter를 실행일로 고정해 Spring Batch의 JobInstance 유일성을 중복 실행 방지에 쓴다
     */
    void runFor(LocalDate runDate) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString(PARAM_RUN_DATE, runDate.toString())
                    .toJobParameters();
            jobLauncher.run(settlementJob, params);
            log.info("결산 배치 실행 완료 - runDate={}", runDate);
        } catch (JobInstanceAlreadyCompleteException e) {
            // 오류가 아니라 정상적인 중복 방지 동작이다.
            log.info("결산 배치 이미 완료됨 - runDate={}", runDate);
        } catch (Exception e) {
            log.error("결산 배치 실행 실패 - runDate={}", runDate, e);
        }
    }
}
