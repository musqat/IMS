package com.ims.inventory.dto.response;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * 재고 소진 예측 결과
 *
 * 기간 내 나간 양으로 월평균을 내고 현재 재고가 몇 달치인지 계산한다.
 * 물류 창고의 재고가 빠지는 속도를 보고 언제 생산을 시작할지 판단하는 용도다.
 *
 * - months: 조회 기간의 개월수. 월평균을 내는 분모다
 */
public record StockDepletionResponse(
        Long warehouseId,
        String warehouseName,
        LocalDate from,
        LocalDate to,
        double months,
        List<StockDepletionRow> rows
) {
    /**
     * rows를 급한 순으로 정렬해 생성한다.
     */
    public static StockDepletionResponse of(
            Long warehouseId, String warehouseName,
            LocalDate from, LocalDate to, double months,
            List<StockDepletionRow> rows) {
        List<StockDepletionRow> sorted = rows.stream()
                .sorted(Comparator.comparing(
                        StockDepletionRow::monthsRemaining,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return new StockDepletionResponse(warehouseId, warehouseName, from, to, months, sorted);
    }
}
