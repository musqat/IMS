import { apiClient, unwrapAs } from './client';
import type { InviteResponse, PartnershipResponse } from '../types';

export const partnershipApi = {
  invite: (companyCode: string): Promise<InviteResponse> =>
    apiClient.post('/partnerships/invite', { companyCode }).then(unwrapAs<InviteResponse>()),

  /** 수신함에서 수락 — 토큰 대신 id로 (sub_id가 초대 시점에 이미 고정돼 있다) */
  acceptById: (partnershipId: number): Promise<PartnershipResponse> =>
    apiClient.post(`/partnerships/${partnershipId}/accept`).then(unwrapAs<PartnershipResponse>()),

  getReceivedInvites: (): Promise<PartnershipResponse[]> =>
    apiClient.get('/partnerships/invites/received').then(unwrapAs<PartnershipResponse[]>()),

  getSentInvites: (): Promise<PartnershipResponse[]> =>
    apiClient.get('/partnerships/invites/sent').then(unwrapAs<PartnershipResponse[]>()),

  cancelInvite: (partnershipId: number): Promise<void> =>
    apiClient.delete(`/partnerships/${partnershipId}/invite`).then(() => {}),

  getSubs: (): Promise<PartnershipResponse[]> =>
    apiClient.get('/partnerships/subs').then(unwrapAs<PartnershipResponse[]>()),

  getMains: (): Promise<PartnershipResponse[]> =>
    apiClient.get('/partnerships/mains').then(unwrapAs<PartnershipResponse[]>()),

  updateAlias: (partnershipId: number, alias: string): Promise<PartnershipResponse> =>
    apiClient.patch(`/partnerships/${partnershipId}/alias`, { alias }).then(unwrapAs<PartnershipResponse>()),

  remove: (partnershipId: number): Promise<void> =>
    apiClient.delete(`/partnerships/${partnershipId}`).then(() => {}),
};
