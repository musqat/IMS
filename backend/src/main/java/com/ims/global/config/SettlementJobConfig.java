package com.ims.global.config;

import com.ims.production.entity.ProductionRecord;
import com.ims.production.entity.ProductionStatus;
import com.ims.production.entity.Settlement;
import com.ims.production.repository.ProductionRepository;
import com.ims.production.service.SettlementService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * 자정 결산 배치 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SettlementJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ProductionRepository productionRepository;
    private final SettlementService settlementService;

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public Job settlementJob() {
        return new JobBuilder("settlementJob", jobRepository)
                .start(settlementStep())
                .build();
    }

    @Bean
    public Step settlementStep() {
        return new StepBuilder("settlementStep", jobRepository)
                .tasklet(settlementTasklet(), transactionManager)
                .build();
    }

    /**
     * 결산 Tasklet
     * - 레코드 단위로 예외를 격리한다. REQUIRES_NEW는 롤백만 분리할 뿐
     *   루프가 계속 도는 것까지 보장하지 않는다
     * - 실패 건이 있으면 마지막에 예외를 던져 Step을 실패로 끝낸다.
     *   예외처리를 안하면 JobInstance가 완료 처리되어 같은 날 재실행이 막히기 때문
     */
    @Bean
    @StepScope
    public Tasklet settlementTasklet() {
        return (contribution, chunkContext) -> {
            List<ProductionRecord> pending = productionRepository.findAllByStatus(ProductionStatus.PENDING);
            log.info("[결산 배치] 대상 레코드 수: {}", pending.size());

            int succeeded = 0;
            int failed = 0;

            for (ProductionRecord record : pending) {
                Long recordId = record.getId();
                try {
                    Settlement settlement = settlementService.settle(record);
                    succeeded++;
                    log.info("[결산 배치] 완료 - recordId={}, result={}",
                            recordId, settlement.getResult());
                } catch (Exception e) {
                    failed++;
                    log.error("[결산 배치] 실패 - recordId={}", recordId, e);
                } finally {
                    // REQUIRES_NEW 커밋 후 1차 캐시 초기화 — 대량 처리 시 메모리 누수 방지.
                    entityManager.clear();
                }
            }

            log.info("[결산 배치] 종료 - 성공 {}건, 실패 {}건", succeeded, failed);

            if (failed > 0) {
                throw new IllegalStateException(
                        "결산 실패 %d건 (성공 %d건) — 재실행 가능하도록 Step을 실패 처리한다"
                                .formatted(failed, succeeded));
            }

            return RepeatStatus.FINISHED;
        };
    }
}
