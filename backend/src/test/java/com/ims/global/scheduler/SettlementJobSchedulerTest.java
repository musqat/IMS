package com.ims.global.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * SettlementJobScheduler 단위 테스트
 * - cron이 KST 기준인가: zone이 없으면 UTC 컨테이너(Render)에서 09:05 KST에 실행된다
 * - JobParameter가 실행일인가: currentTimeMillis면 매번 새 JobInstance가 되어 중복 방지가 깨진다
 * - 예외를 삼키는가: 스케줄 메서드에서 예외가 새면 이후 스케줄이 중단될 수 있다
 */
@ExtendWith(MockitoExtension.class)
class SettlementJobSchedulerTest {

    @InjectMocks
    private SettlementJobScheduler scheduler;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job settlementJob;

    @Test
    @DisplayName("cron은 00:05에 Asia/Seoul 기준으로 실행된다")
    void scheduledAnnotation_usesKstZone() throws Exception {
        Method method = SettlementJobScheduler.class.getMethod("runSettlementJob");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("0 5 0 * * *");
        // zone이 비어 있으면 JVM 기본 타임존(배포 환경에서는 UTC)을 따른다
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }

    @Test
    @DisplayName("JobParameter로 실행일을 넘긴다")
    void runFor_passesRunDateAsParameter() throws Exception {
        // given
        LocalDate runDate = LocalDate.of(2026, 8, 16);

        // when
        scheduler.runFor(runDate);

        // then
        ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
        then(jobLauncher).should().run(eq(settlementJob), captor.capture());

        JobParameters params = captor.getValue();
        assertThat(params.getString(SettlementJobScheduler.PARAM_RUN_DATE))
                .isEqualTo("2026-08-16");
        // 매 실행마다 값이 달라지는 파라미터가 없어야 JobInstance 유일성이 성립한다
        assertThat(params.getParameters()).hasSize(1);
    }

    @Test
    @DisplayName("같은 날짜로 두 번 호출하면 동일한 JobParameters가 만들어진다")
    void runFor_sameDate_producesSameParameters() throws Exception {
        // given
        LocalDate runDate = LocalDate.of(2026, 8, 16);

        // when
        scheduler.runFor(runDate);
        scheduler.runFor(runDate);

        // then
        ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
        then(jobLauncher).should(times(2)).run(eq(settlementJob), captor.capture());

        assertThat(captor.getAllValues().get(0))
                .isEqualTo(captor.getAllValues().get(1));
    }

    @Test
    @DisplayName("이미 완료된 JobInstance면 예외를 삼키고 정상 종료한다")
    void runFor_alreadyComplete_doesNotPropagate() throws Exception {
        // given — 중복 실행 방지가 동작한 상황. 오류가 아니다.
        willThrow(new JobInstanceAlreadyCompleteException("already complete"))
                .given(jobLauncher).run(any(), any());

        // when & then
        assertThatNoException().isThrownBy(() -> scheduler.runFor(LocalDate.of(2026, 8, 16)));
    }

    @Test
    @DisplayName("Job 실행이 실패해도 예외를 밖으로 던지지 않는다")
    void runFor_jobFails_doesNotPropagate() throws Exception {
        // given — 스케줄 메서드에서 예외가 새면 이후 스케줄이 멈출 수 있다
        willThrow(new IllegalStateException("결산 실패 1건"))
                .given(jobLauncher).run(any(), any());

        // when & then
        assertThatNoException().isThrownBy(() -> scheduler.runFor(LocalDate.of(2026, 8, 16)));
    }

    @Test
    @DisplayName("스케줄 진입점은 오늘(KST) 날짜로 실행한다")
    void runSettlementJob_usesTodayInKst() throws Exception {
        // when
        scheduler.runSettlementJob();

        // then
        ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
        then(jobLauncher).should().run(eq(settlementJob), captor.capture());

        String expected = LocalDate.now(SettlementJobScheduler.KST).toString();
        assertThat(captor.getValue().getString(SettlementJobScheduler.PARAM_RUN_DATE))
                .isEqualTo(expected);
    }
}
