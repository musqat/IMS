import { apiClient, unwrapAs } from './client';
import type { LoginResponse, UserResponse } from '../types';

export const authApi = {
  login: (email: string, password: string): Promise<LoginResponse> =>
    apiClient.post('/users/login', { email, password }).then(unwrapAs<LoginResponse>()),

  // 회원가입 즉시 로그인 — 백엔드가 accessToken/refreshToken 바로 반환
  register: (email: string, password: string, companyName: string): Promise<LoginResponse> =>
    apiClient.post('/users/register', { email, password, companyName }).then(unwrapAs<LoginResponse>()),

  logout: (): Promise<void> =>
    apiClient.post('/users/logout').then(unwrapAs<void>()),

  getMe: (): Promise<UserResponse> =>
    apiClient.get('/users/me').then(unwrapAs<UserResponse>()),

  updateCompanyName: (companyName: string): Promise<UserResponse> =>
    apiClient.patch('/users/me/company-name', { companyName }).then(unwrapAs<UserResponse>()),

  updatePassword: (currentPassword: string, newPassword: string): Promise<void> =>
    apiClient.patch('/users/me/password', { currentPassword, newPassword }).then(unwrapAs<void>()),
};
