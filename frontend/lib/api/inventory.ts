import { apiClient, unwrapAs } from './client';
import type { InventoryResponse, InventoryHistoryResponse, InventoryExportRow, InventoryHistoryType, MaxProducibleResponse, ShortageItemResponse, StockDepletionResponse, Page } from '../types';

export const inventoryApi = {
  getList: (warehouseId: number, keyword?: string, page = 0, size = 20): Promise<Page<InventoryResponse>> =>
    apiClient.get(`/warehouses/${warehouseId}/inventories`, {
      params: { keyword, page, size },
    }).then(unwrapAs<Page<InventoryResponse>>()),

  create: (warehouseId: number, itemId: number, quantity: number, safetyStock: number): Promise<InventoryResponse> =>
    apiClient.post(`/warehouses/${warehouseId}/inventories`, { itemId, quantity, safetyStock }).then(unwrapAs<InventoryResponse>()),

  stockIn: (warehouseId: number, itemId: number, quantity: number, memo?: string): Promise<InventoryResponse> =>
    apiClient.post(`/warehouses/${warehouseId}/inventories/${itemId}/in`, { quantity, memo }).then(unwrapAs<InventoryResponse>()),

  stockOut: (warehouseId: number, itemId: number, quantity: number, memo?: string): Promise<InventoryResponse> =>
    apiClient.post(`/warehouses/${warehouseId}/inventories/${itemId}/out`, { quantity, memo }).then(unwrapAs<InventoryResponse>()),

  adjust: (warehouseId: number, itemId: number, quantity: number, memo?: string): Promise<InventoryResponse> =>
    apiClient.put(`/warehouses/${warehouseId}/inventories/${itemId}/adjust`, { quantity, memo }).then(unwrapAs<InventoryResponse>()),

  getHistory: (warehouseId: number, itemId: number, page = 0, size = 20): Promise<Page<InventoryHistoryResponse>> =>
    apiClient.get(`/warehouses/${warehouseId}/inventories/${itemId}/history`, {
      params: { page, size },
    }).then(unwrapAs<Page<InventoryHistoryResponse>>()),

  getMaxProducible: (warehouseId: number, itemId: number): Promise<MaxProducibleResponse> =>
    apiClient.get(`/warehouses/${warehouseId}/inventories/${itemId}/max-producible`).then(unwrapAs<MaxProducibleResponse>()),

  updateSafetyStock: (warehouseId: number, itemId: number, safetyStock: number): Promise<InventoryResponse> =>
    apiClient.patch(`/warehouses/${warehouseId}/inventories/${itemId}/safety-stock`, { safetyStock }).then(unwrapAs<InventoryResponse>()),

  /** 창고 전체 이력 조회 (피벗 Export용) */
  getWarehouseHistory: (
    warehouseId: number,
    types: InventoryHistoryType[],
    from: string,
    to: string,
  ): Promise<InventoryExportRow[]> =>
    apiClient.get(`/warehouses/${warehouseId}/inventories/histories`, {
      params: { types, from, to },
    }).then(unwrapAs<InventoryExportRow[]>()),

  getDepletion: (warehouseId: number, from: string, to: string): Promise<StockDepletionResponse> =>
    apiClient.get(`/warehouses/${warehouseId}/inventories/depletion`, { params: { from, to } })
      .then(unwrapAs<StockDepletionResponse>()),

  getShortageAnalysis: (warehouseId: number): Promise<ShortageItemResponse[]> =>
    apiClient.get(`/warehouses/${warehouseId}/inventories/shortage-analysis`)
      .then(unwrapAs<ShortageItemResponse[]>()),
};
