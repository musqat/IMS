import { apiClient, unwrapAs } from './client';
import type { ItemResponse, BomResponse, ItemType } from '../types';

export const itemApi = {
  getList: (): Promise<ItemResponse[]> =>
    apiClient.get('/items').then(unwrapAs<ItemResponse[]>()),

  getOne: (id: number): Promise<ItemResponse> =>
    apiClient.get(`/items/${id}`).then(unwrapAs<ItemResponse>()),

  create: (itemCode: string, name: string, type: ItemType, description?: string): Promise<ItemResponse> =>
    apiClient.post('/items', { itemCode, name, type, description }).then(unwrapAs<ItemResponse>()),

  delete: (id: number): Promise<void> =>
    apiClient.delete(`/items/${id}`).then(unwrapAs<void>()),

  getBoms: (itemId: number): Promise<BomResponse[]> =>
    apiClient.get(`/items/${itemId}/bom`).then(unwrapAs<BomResponse[]>()),

  addBom: (parentItemId: number, childItemId: number, quantity: number): Promise<BomResponse> =>
    apiClient.post(`/items/${parentItemId}/bom`, { childItemId, quantity }).then(unwrapAs<BomResponse>()),

  deleteBom: (parentItemId: number, bomId: number): Promise<void> =>
    apiClient.delete(`/items/${parentItemId}/bom/${bomId}`).then(unwrapAs<void>()),
};
