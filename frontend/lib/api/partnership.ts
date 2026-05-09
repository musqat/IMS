import { apiClient, unwrapAs } from './client';
import type { InviteResponse, PartnershipResponse } from '../types';

export const partnershipApi = {
  invite: (companyCode: string): Promise<InviteResponse> =>
    apiClient.post('/partnerships/invite', { companyCode }).then(unwrapAs<InviteResponse>()),

  accept: (token: string): Promise<PartnershipResponse> =>
    apiClient.post('/partnerships/accept', null, { params: { token } }).then(unwrapAs<PartnershipResponse>()),

  getSubs: (): Promise<PartnershipResponse[]> =>
    apiClient.get('/partnerships/subs').then(unwrapAs<PartnershipResponse[]>()),

  getMains: (): Promise<PartnershipResponse[]> =>
    apiClient.get('/partnerships/mains').then(unwrapAs<PartnershipResponse[]>()),

  updateAlias: (partnershipId: number, alias: string): Promise<PartnershipResponse> =>
    apiClient.patch(`/partnerships/${partnershipId}/alias`, { alias }).then(unwrapAs<PartnershipResponse>()),

  remove: (partnershipId: number): Promise<void> =>
    apiClient.delete(`/partnerships/${partnershipId}`).then(() => {}),
};
