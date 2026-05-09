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

  getShared: (): Promise<WarehouseShareResponse[]> =>
    apiClient.get('/warehouses/shared').then(unwrapAs<WarehouseShareResponse[]>()),

  share: (warehouseId: number, companyCode: string, permission: 'VIEW' | 'FULL'): Promise<WarehouseShareResponse> =>
    apiClient.post(`/warehouses/${warehouseId}/shares`, { companyCode, permission }).then(unwrapAs<WarehouseShareResponse>()),

  revokeShare: (warehouseId: number, companyCode: string): Promise<void> =>
    apiClient.delete(`/warehouses/${warehouseId}/shares`, { params: { companyCode } }).then(unwrapAs<void>()),
};
