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
     */
    @Bean
    @StepScope
    public Tasklet settlementTasklet() {
        return (contribution, chunkContext) -> {
            List<ProductionRecord> pending = productionRepository.findAllByStatus(ProductionStatus.PENDING);
            log.info("[결산 배치] 대상 레코드 수: {}", pending.size());

            for (ProductionRecord record : pending) {
                Settlement settlement = settlementService.settle(record);
                log.info("[결산 배치] 완료 - recordId={}, result={}",
                        record.getId(), settlement.getResult());
                // REQUIRES_NEW 커밋 후 1차 캐시 초기화 — 대량 처리 시 메모리 누수 방지
                entityManager.clear();
            }

            return RepeatStatus.FINISHED;
        };
    }
}
