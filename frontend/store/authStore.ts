import { create } from 'zustand';

interface AuthUser {
  id: number;
  email: string;
  companyName: string;
  companyCode: string;
}

interface AuthStore {
  user: AuthUser | null;
  isAuthenticated: boolean;
  setUser: (user: AuthUser) => void;
  clearAuth: () => void;
  updateCompanyName: (companyName: string) => void;
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  isAuthenticated: false,

  /**
   * 사용자 상태만 갱신한다. 토큰은 저장하지 않는다.
   *
   * 토큰을 쓰는 곳은 로그인(useLoginSession)과 갱신(api/client 인터셉터) 두 곳뿐이다.
   */
  setUser: (user) => set({ user, isAuthenticated: true }),

  clearAuth: () => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
    }
    set({ user: null, isAuthenticated: false });
  },

  updateCompanyName: (companyName) =>
    set((state) => ({
      user: state.user ? { ...state.user, companyName } : null,
    })),
}));

interface SidebarStore {
  collapsed: boolean;
  toggle: () => void;
}

export const useSidebarStore = create<SidebarStore>((set) => ({
  collapsed: false,
  toggle: () => set((state) => ({ collapsed: !state.collapsed })),
}));
