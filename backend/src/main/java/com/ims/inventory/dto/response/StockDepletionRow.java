package com.ims.inventory.dto.response;

/**
 * 재고 소진 예측 — 품목 한 줄
 * - totalOutbound: 기간 내 나간 총량 (OUT + PRODUCTION_DEDUCTION, 양수로 환산)
 * - monthlyAverage: totalOutbound / 기간 개월수
 * - monthsRemaining: currentStock / monthlyAverage. 소진이 없으면 null(줄지 않는다)
 */
public record StockDepletionRow(
        Long itemId,
        String itemCode,
        String itemName,
        int currentStock,
        int safetyStock,
        int totalOutbound,
        Double monthlyAverage,
        Double monthsRemaining
) {
    /**
     * 월평균과 잔여 개월을 계산해 생성한다.
     */
    public static StockDepletionRow of(
            Long itemId, String itemCode, String itemName,
            int currentStock, int safetyStock, int totalOutbound, double months) {
        double monthlyAverage = totalOutbound / months;
        Double monthsRemaining = monthlyAverage == 0 ? null : currentStock / monthlyAverage;

        return new StockDepletionRow(
                itemId, itemCode, itemName, currentStock, safetyStock, totalOutbound, monthlyAverage, monthsRemaining);
    }
}
