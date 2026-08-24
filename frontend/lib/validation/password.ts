import { z } from 'zod';

/**
 * 비밀번호 정책
 *
 * 백엔드 `PasswordPolicy`와 같은 규칙이다.
 * 두 쪽 기준이 다르면 화면에서 통과한 값이 서버에서 400이 된다.
 */
export const PASSWORD_MESSAGE = '비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다';

export const passwordField = z
  .string()
  .min(8, PASSWORD_MESSAGE)
  // 특수문자는 강제하지 않는다. 길이와 혼용만으로도 흔한 비밀번호는 걸러진다
  .regex(/^(?=.*[A-Za-z])(?=.*\d).+$/, PASSWORD_MESSAGE);
