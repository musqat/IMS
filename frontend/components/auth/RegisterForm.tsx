'use client';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { authApi } from '@/lib/api/auth';
import { useLoginSession } from '@/hooks/useLoginSession';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import { passwordField } from '@/lib/validation/password';

const schema = z.object({
  companyName: z.string().min(1, '회사명을 입력하세요'),
  email: z.string().email('올바른 이메일을 입력하세요'),
  password: passwordField,
});
type FormData = z.infer<typeof schema>;

export function RegisterForm() {
  const { loginWithTokens } = useLoginSession();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    try {
      // 회원가입 — 백엔드가 토큰 즉시 반환 (auto-login)
      const tokens = await authApi.register(data.email, data.password, data.companyName);
      await loginWithTokens(tokens);
      toast.success('회원가입이 완료되었습니다!');
    } catch {
      toast.error('회원가입에 실패했습니다. 이미 사용 중인 이메일일 수 있습니다.');
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="space-y-1">
        <Label htmlFor="companyName">회사명</Label>
        <Input
          id="companyName"
          placeholder="(주)한국제조"
          {...register('companyName')}
        />
        {errors.companyName && (
          <p className="text-xs text-rose-500">{errors.companyName.message}</p>
        )}
      </div>
      <div className="space-y-1">
        <Label htmlFor="email">이메일</Label>
        <Input
          id="email"
          type="email"
          placeholder="company@example.com"
          {...register('email')}
        />
        {errors.email && (
          <p className="text-xs text-rose-500">{errors.email.message}</p>
        )}
      </div>
      <div className="space-y-1">
        <Label htmlFor="password">비밀번호</Label>
        <Input
          id="password"
          type="password"
          placeholder="8자 이상, 영문+숫자"
          {...register('password')}
        />
        {errors.password && (
          <p className="text-xs text-rose-500">{errors.password.message}</p>
        )}
      </div>
      <Button type="submit" className="w-full" disabled={isSubmitting}>
        {isSubmitting ? '가입 중...' : '회원가입'}
      </Button>
    </form>
  );
}
