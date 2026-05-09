import { LoginForm } from '@/components/auth/LoginForm';
import Link from 'next/link';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';

export default function LoginPage() {
  return (
    <main className="min-h-screen bg-stone-50 flex items-center justify-center px-4">
      <Card className="w-full max-w-sm">
        <CardHeader className="text-center">
          <div className="w-8 h-8 rounded-md bg-stone-900 flex items-center justify-center text-white font-bold text-sm mx-auto mb-2">
            I
          </div>
          <CardTitle>로그인</CardTitle>
          <CardDescription>IMS 계정으로 로그인하세요</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <LoginForm />
          <p className="text-center text-sm text-stone-500">
            계정이 없으신가요?{' '}
            <Link
              href="/register"
              className="text-violet-600 hover:underline font-medium"
            >
              회원가입
            </Link>
          </p>
        </CardContent>
      </Card>
    </main>
  );
}
