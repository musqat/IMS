package com.ims.global.config;

import com.ims.production.entity.ProductionRecord;
import com.ims.production.entity.ProductionStatus;
import com.ims.production.entity.Settlement;
import com.ims.production.repository.ProductionRepository;
import com.ims.production.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SettlementJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ProductionRepository productionRepository;
    private final SettlementService settlementService;

    @Bean
    public Job settlementJob() {
        return new JobBuilder("settlementJob", jobRepository)
                .start(settlementStep())
                .build();
    }

    @Bean
    public Step settlementStep() {
        return new StepBuilder("settlementStep", jobRepository)
                .<ProductionRecord, Settlement>chunk(10, transactionManager)
                .reader(settlementItemReader())
                .processor(settlementItemProcessor())
                .writer(settlementItemWriter())
                .build();
    }

    @Bean
    @StepScope
    public ListItemReader<ProductionRecord> settlementItemReader() {
        return new ListItemReader<>(
                productionRepository.findAllByStatus(ProductionStatus.PENDING)
        );
    }

    @Bean
    public ItemProcessor<ProductionRecord, Settlement> settlementItemProcessor() {
        return settlementService::settle;
    }

    @Bean
    public ItemWriter<Settlement> settlementItemWriter() {
        return items -> items.forEach(s ->
                log.info("결산 완료 - productionRecordId={}, result={}", s.getProductionRecord().getId(), s.getResult())
        );
    }
}
