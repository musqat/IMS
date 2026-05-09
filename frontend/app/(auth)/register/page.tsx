import { RegisterForm } from '@/components/auth/RegisterForm';
import Link from 'next/link';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';

export default function RegisterPage() {
  return (
    <main className="min-h-screen bg-stone-50 flex items-center justify-center px-4">
      <Card className="w-full max-w-sm">
        <CardHeader className="text-center">
          <div className="w-8 h-8 rounded-md bg-stone-900 flex items-center justify-center text-white font-bold text-sm mx-auto mb-2">
            I
          </div>
          <CardTitle>회원가입</CardTitle>
          <CardDescription>새 IMS 계정을 만드세요</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <RegisterForm />
          <p className="text-center text-sm text-stone-500">
            이미 계정이 있으신가요?{' '}
            <Link
              href="/login"
              className="text-violet-600 hover:underline font-medium"
            >
              로그인
            </Link>
          </p>
        </CardContent>
      </Card>
    </main>
  );
}
