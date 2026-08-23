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
  /** PENDING 초대의 만료 시각. 수락된 초대와 TTL 이전 데이터는 null */
  inviteExpiresAt: string | null;
}

export interface WarehouseResponse {
  id: number;
  name: string;
  location: string;
  ownerId: number;
  ownerCompanyName: string;
  /** false면 비활성 창고. 목록에서 숨겨지고 쓰기가 막히되 상세 조회는 가능하다 */
  active: boolean;
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
  /** BOM이 없으면 차감할 부품이 없다는 뜻이라 null(제한 없음)이 온다 */
  maxQuantity: number | null;
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
  total: number; // pending + settled + cancelled
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

/**
 * 소유 창고와 공유받은 창고를 한 목록으로 다루기 위한 뷰 모델.
 * 두 API의 응답 형태가 달라(WarehouseResponse / WarehouseShareResponse)
 * 화면에서 매번 분기하지 않도록 공통 필드만 추린다.
 */
export interface AccessibleWarehouse {
  id: number;
  name: string;
  location: string;
  ownerId: number;
  ownerCompanyName: string;
  /** 공유받은 창고면 true. 소유 창고는 false */
  isShared: boolean;
  /** 소유 창고는 항상 FULL로 취급한다 */
  permission: 'VIEW' | 'FULL';
}

export interface StockDepletionRow {
  itemId: number;
  itemCode: string;
  itemName: string;
  currentStock: number;
  safetyStock: number;
  totalOutbound: number;
  monthlyAverage: number;
  /** 기간 내 소진이 없으면 null. 0으로 오해하면 "곧 소진"으로 읽힌다 */
  monthsRemaining: number | null;
}

export interface StockDepletionResponse {
  warehouseId: number;
  warehouseName: string;
  from: string;
  to: string;
  months: number;
  rows: StockDepletionRow[];
}
