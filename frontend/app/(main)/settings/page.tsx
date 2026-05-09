'use client';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import { authApi } from '@/lib/api/auth';
import { getApiError } from '@/lib/api/client';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';

const companySchema = z.object({
  companyName: z.string().min(1, '회사명을 입력하세요'),
});

const passwordSchema = z.object({
  currentPassword: z.string().min(1, '현재 비밀번호를 입력하세요'),
  newPassword: z.string().min(6, '비밀번호는 6자 이상이어야 합니다'),
});

type CompanyForm = z.infer<typeof companySchema>;
type PasswordForm = z.infer<typeof passwordSchema>;

export default function SettingsPage() {
  const { user, updateCompanyName } = useAuthStore();

  const companyForm = useForm<CompanyForm>({
    resolver: zodResolver(companySchema),
    defaultValues: { companyName: user?.companyName ?? '' },
  });

  const passwordForm = useForm<PasswordForm>({
    resolver: zodResolver(passwordSchema),
    defaultValues: { currentPassword: '', newPassword: '' },
  });

  const companyMutation = useMutation({
    mutationFn: (data: CompanyForm) => authApi.updateCompanyName(data.companyName),
    onSuccess: (updated) => {
      updateCompanyName(updated.companyName);
      toast.success('회사명이 변경되었습니다.');
    },
    onError: (error) => toast.error(getApiError(error, '회사명 변경에 실패했습니다.')),
  });

  const passwordMutation = useMutation({
    mutationFn: (data: PasswordForm) =>
      authApi.updatePassword(data.currentPassword, data.newPassword),
    onSuccess: () => {
      toast.success('비밀번호가 변경되었습니다.');
      passwordForm.reset();
    },
    onError: (error) =>
      toast.error(getApiError(error, '비밀번호 변경에 실패했습니다. 현재 비밀번호를 확인하세요.')),
  });

  return (
    <div className="max-w-lg space-y-6">
      <h1 className="text-2xl font-bold text-stone-900">설정</h1>

      {/* 계정 정보 */}
      <Card className="border-stone-200">
        <CardHeader>
          <CardTitle className="text-base">계정 정보</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div>
            <Label className="text-stone-500 text-xs">이메일</Label>
            <p className="text-stone-800 font-medium">{user?.email}</p>
          </div>
          <div>
            <Label className="text-stone-500 text-xs">회사 코드</Label>
            <p className="text-stone-800 font-mono">{user?.companyCode}</p>
          </div>
        </CardContent>
      </Card>

      {/* 회사명 수정 */}
      <Card className="border-stone-200">
        <CardHeader>
          <CardTitle className="text-base">회사명 수정</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={companyForm.handleSubmit((d) => companyMutation.mutate(d))} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="companyName">회사명</Label>
              <Input id="companyName" {...companyForm.register('companyName')} />
              {companyForm.formState.errors.companyName && (
                <p className="text-rose-600 text-xs">{companyForm.formState.errors.companyName.message}</p>
              )}
            </div>
            <Button type="submit" disabled={companyMutation.isPending}>저장</Button>
          </form>
        </CardContent>
      </Card>

      {/* 비밀번호 변경 */}
      <Card className="border-stone-200">
        <CardHeader>
          <CardTitle className="text-base">비밀번호 변경</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={passwordForm.handleSubmit((d) => passwordMutation.mutate(d))} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="currentPassword">현재 비밀번호</Label>
              <Input id="currentPassword" type="password" {...passwordForm.register('currentPassword')} />
              {passwordForm.formState.errors.currentPassword && (
                <p className="text-rose-600 text-xs">{passwordForm.formState.errors.currentPassword.message}</p>
              )}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="newPassword">새 비밀번호</Label>
              <Input id="newPassword" type="password" {...passwordForm.register('newPassword')} />
              {passwordForm.formState.errors.newPassword && (
                <p className="text-rose-600 text-xs">{passwordForm.formState.errors.newPassword.message}</p>
              )}
            </div>
            <Button type="submit" disabled={passwordMutation.isPending}>변경</Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
