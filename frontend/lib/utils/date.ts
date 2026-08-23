/**
 * Date를 로컬 기준 YYYY-MM-DD 문자열로 변환한다.
 * toISOString()은 UTC로 변환하므로 KST 오전 9시 이전에는 날짜가 하루 밀린다.
 * 화면에 보이는 날짜와 서버로 보내는 날짜가 어긋나므로 로컬 필드를 직접 조합한다.
 */
export function toLocalDateString(d: Date): string {
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${month}-${day}`;
}

/**
 * 만료까지 남은 일수. 이미 지났으면 음수.
 * 초대 수신함에서 "3일 남음" / "만료됨"을 가르는 데 쓴다.
 */
export function daysUntil(expiresAt: string | null): number | null {
  if (!expiresAt) return null;
  const diffMs = new Date(expiresAt).getTime() - Date.now();
  return Math.ceil(diffMs / (1000 * 60 * 60 * 24));
}

/** 오늘로부터 n일 전 (로컬 기준) */
export function daysAgo(n: number): string {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return toLocalDateString(d);
}

/** 오늘로부터 n개월 전 (로컬 기준) */
export function monthsAgo(n: number): string {
  const d = new Date();
  d.setMonth(d.getMonth() - n);
  return toLocalDateString(d);
}
