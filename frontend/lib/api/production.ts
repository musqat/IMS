import { apiClient, unwrapAs } from './client';
import type { ProductionCounts, ProductionResponse, ProductionStatus, SettlementResult, Page } from '../types';

export const productionApi = {
  /** 상태별 + ANOMALY 건수 집계 */
  getCounts: (): Promise<ProductionCounts> =>
    apiClient.get('/productions/counts')
      .then(unwrapAs<ProductionCounts>()),

  /** 상태 필터 + 서버 페이지네이션 */
  getByStatus: (status: ProductionStatus, page = 0, size = 30): Promise<Page<ProductionResponse>> =>
    apiClient.get('/productions', { params: { status, page, size } })
      .then(unwrapAs<Page<ProductionResponse>>()),

  getByWarehouse: (warehouseId: number, page = 0, size = 50): Promise<Page<ProductionResponse>> =>
    apiClient.get(`/warehouses/${warehouseId}/productions`, {
      params: { page, size },
    }).then(unwrapAs<Page<ProductionResponse>>()),

  create: (warehouseId: number, itemId: number, quantity: number): Promise<ProductionResponse> =>
    apiClient.post(`/warehouses/${warehouseId}/productions`, { itemId, quantity }).then(unwrapAs<ProductionResponse>()),

  cancel: (warehouseId: number, recordId: number): Promise<void> =>
    apiClient.delete(`/warehouses/${warehouseId}/productions/${recordId}`).then(unwrapAs<void>()),

  update: (warehouseId: number, recordId: number, quantity: number): Promise<ProductionResponse> =>
    apiClient.patch(`/warehouses/${warehouseId}/productions/${recordId}`, { quantity }).then(unwrapAs<ProductionResponse>()),

  forceSettle: (warehouseId: number, recordId: number): Promise<ProductionResponse> =>
    apiClient.post(`/warehouses/${warehouseId}/productions/${recordId}/settle`).then(unwrapAs<ProductionResponse>()),

  updateSettlement: (
    warehouseId: number,
    recordId: number,
    result: SettlementResult,
    memo: string
  ): Promise<ProductionResponse> =>
    apiClient.patch(`/warehouses/${warehouseId}/productions/${recordId}/settlement`, { result, memo })
      .then(unwrapAs<ProductionResponse>()),
};
