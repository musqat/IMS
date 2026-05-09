// 백엔드 ApiResponse<T> 래퍼
export interface ApiResponse<T> {
  message: string;
  data: T;
}

// 백엔드 Page<T> (Spring Pageable)
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
}

export interface UserResponse {
  id: number;
  email: string;
  companyName: string;
  companyCode: string;
}

export interface InviteResponse {
  partnershipId: number;
  inviteToken: string;
}

export interface PartnershipResponse {
  id: number;
  mainId: number;
  mainCompanyName: string;
  subId: number;
  subCompanyName: string;
  subCompanyCode: string;
  status: 'PENDING' | 'ACCEPTED';
  acceptedAt: string | null;
  alias: string | null;
}

export interface WarehouseResponse {
  id: number;
  name: string;
  location: string;
  ownerId: number;
  ownerCompanyName: string;
  createdAt: string;
}

export interface WarehouseShareResponse {
  id: number;
  warehouseId: number;
  warehouseName: string;
  warehouseLocation: string;
  ownerId: number;
  ownerCompanyName: string;
  sharedWithId: number;
  sharedWithCompanyName: string;
  permission: 'VIEW' | 'FULL';
}

export type ItemType = 'PRODUCT' | 'SEMI' | 'PART';

export interface InventoryResponse {
  id: number;
  warehouseId: number;
  itemId: number;
  itemCode: string;
  itemName: string;
  itemType: ItemType;
  quantity: number;
  safetyStock: number;
  warning: string | null;
}

export type InventoryHistoryType = 'IN' | 'OUT' | 'ADJUSTMENT' | 'PRODUCTION_DEDUCTION';

export interface PartShortageDto {
  partId: number;
  partCode: string;
  partName: string;
  requiredPerUnit: number;
  currentStock: number;
}

export interface ShortageItemResponse {
  itemId: number;
  itemCode: string;
  itemName: string;
  shortages: PartShortageDto[];
}

export interface InventoryHistoryResponse {
  id: number;
  type: InventoryHistoryType;
  delta: number;
  memo: string | null;
  createdAt: string;
}

export interface InventoryExportRow {
  itemCode: string;
  itemName: string;
  type: InventoryHistoryType;
  delta: number;
  date: string; // 'YYYY-MM-DD'
}

export interface MaxProducibleResponse {
  itemId: number;
  itemName: string;
  maxQuantity: number;
}

export interface ItemResponse {
  id: number;
  itemCode: string;
  name: string;
  type: ItemType;
  description: string | null;
}

export interface BomResponse {
  id: number;
  parentItemId: number;
  parentItemCode: string;
  parentItemName: string;
  childItemId: number;
  childItemCode: string;
  childItemName: string;
  quantity: number;
}

export type ProductionStatus = 'PENDING' | 'SETTLED' | 'CANCELLED';

export interface ProductionCounts {
  pending: number;
  settled: number;
  cancelled: number;
  anomaly: number;
  total: number; // 백엔드 record의 total() 메서드가 Jackson에 의해 직렬화됨
}
export type SettlementResult = 'SUCCESS' | 'ANOMALY' | 'FAILED';

export interface SettlementResponse {
  id: number;
  result: SettlementResult;
  anomalyDetail: string | null;
  memo: string | null;
  settledAt: string;
}

export interface ProductionResponse {
  id: number;
  warehouseId: number;
  itemId: number;
  itemName: string;
  quantity: number;
  status: ProductionStatus;
  settlement: SettlementResponse | null;
  createdAt: string;
}
