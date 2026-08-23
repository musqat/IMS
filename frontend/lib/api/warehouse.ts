import { apiClient, unwrapAs } from './client';
import type { WarehouseResponse, WarehouseShareResponse } from '../types';

export const warehouseApi = {
  getList: (): Promise<WarehouseResponse[]> =>
    apiClient.get('/warehouses').then(unwrapAs<WarehouseResponse[]>()),

  getOne: (id: number): Promise<WarehouseResponse> =>
    apiClient.get(`/warehouses/${id}`).then(unwrapAs<WarehouseResponse>()),

  create: (name: string, location: string): Promise<WarehouseResponse> =>
    apiClient.post('/warehouses', { name, location }).then(unwrapAs<WarehouseResponse>()),

  delete: (id: number): Promise<void> =>
    apiClient.delete(`/warehouses/${id}`).then(unwrapAs<void>()),

  /** 비활성 창고 목록 (활성화용) */
  getInactive: (): Promise<WarehouseResponse[]> =>
    apiClient.get('/warehouses/inactive').then(unwrapAs<WarehouseResponse[]>()),

  /** 창고 비활성화 — 목록에서 숨기고 쓰기를 막되 이력은 보존한다 */
  deactivate: (id: number): Promise<void> =>
    apiClient.patch(`/warehouses/${id}/deactivate`).then(unwrapAs<void>()),

  activate: (id: number): Promise<void> =>
    apiClient.patch(`/warehouses/${id}/activate`).then(unwrapAs<void>()),

  getShared: (): Promise<WarehouseShareResponse[]> =>
    apiClient.get('/warehouses/shared').then(unwrapAs<WarehouseShareResponse[]>()),

  share: (warehouseId: number, companyCode: string, permission: 'VIEW' | 'FULL'): Promise<WarehouseShareResponse> =>
    apiClient.post(`/warehouses/${warehouseId}/shares`, { companyCode, permission }).then(unwrapAs<WarehouseShareResponse>()),

  revokeShare: (warehouseId: number, companyCode: string): Promise<void> =>
    apiClient.delete(`/warehouses/${warehouseId}/shares`, { params: { companyCode } }).then(unwrapAs<void>()),
};
