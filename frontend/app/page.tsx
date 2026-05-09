import Link from 'next/link';
import { buttonVariants } from '@/components/ui/button';
import { cn } from '@/lib/utils';

export default function LandingPage() {
  return (
    <main className="min-h-screen bg-stone-50">
      {/* 헤더 */}
      <header className="border-b border-stone-200 bg-white">
        <div className="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-md bg-stone-900 flex items-center justify-center text-white font-bold text-xs">
              I
            </div>
            <span className="font-semibold text-stone-900">IMS</span>
          </div>
          <div className="flex gap-2">
            <Link href="/login" className={cn(buttonVariants({ variant: 'ghost' }))}>
              로그인
            </Link>
            <Link href="/register" className={cn(buttonVariants({ variant: 'default' }))}>
              회원가입
            </Link>
          </div>
        </div>
      </header>

      {/* 히어로 섹션 */}
      <section className="max-w-5xl mx-auto px-6 py-24 text-center">
        <h1 className="text-5xl font-bold text-stone-900 mb-4">
          제조업 협업 재고관리
        </h1>
        <p className="text-xl text-stone-500 mb-12 max-w-2xl mx-auto">
          본사와 하청사가 함께 창고, 재고, 생산 결산을 실시간으로 관리합니다.
        </p>
        <div className="flex gap-4 justify-center">
          <Link href="/register" className={cn(buttonVariants({ size: 'lg' }))}>
            시작하기
          </Link>
          <Link href="/login" className={cn(buttonVariants({ variant: 'outline', size: 'lg' }))}>
            로그인
          </Link>
        </div>
      </section>

      {/* 기능 소개 */}
      <section className="max-w-5xl mx-auto px-6 pb-24">
        <div className="grid grid-cols-3 gap-6">
          {[
            {
              title: '본사-하청 협업',
              desc: '초대 링크로 파트너 관계를 맺고 창고와 재고를 실시간 공유합니다.',
            },
            {
              title: '자재 명세 기반 생산 결산',
              desc: '부품 구조(자재 명세)를 기반으로 매일 자정 생산 결산을 자동 처리합니다.',
            },
            {
              title: '실시간 재고 관리',
              desc: '입고·출고·조정 이력을 추적하고 안전재고 미달 시 즉시 경고합니다.',
            },
          ].map((f) => (
            <div key={f.title} className="bg-white rounded-2xl border border-stone-200 p-6">
              <h3 className="font-semibold text-stone-900 mb-2">{f.title}</h3>
              <p className="text-sm text-stone-500">{f.desc}</p>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
