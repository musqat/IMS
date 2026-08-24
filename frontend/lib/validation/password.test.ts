import { describe, it, expect } from 'vitest';
import { passwordField } from './password';

/**
 * 백엔드 PasswordPolicy와 같은 규칙이어야 한다.
 * 두 쪽이 어긋나면 화면에서 통과한 값이 서버에서 400이 된다.
 */
describe('passwordField', () => {
  // 정책을 넣기 전 실제로 201로 가입되던 값들이다
  it.each(['123456', 'aaaaaa', 'password', '12345678'])('%s 는 거부한다', (pw) => {
    expect(passwordField.safeParse(pw).success).toBe(false);
  });

  it.each(['Test1234!', 'abcd1234', 'a1234567'])('%s 는 통과한다', (pw) => {
    expect(passwordField.safeParse(pw).success).toBe(true);
  });

  it('8자 미만은 영문·숫자를 섞어도 거부한다', () => {
    expect(passwordField.safeParse('ab12345').success).toBe(false);
  });

  it('특수문자는 강제하지 않는다', () => {
    expect(passwordField.safeParse('abcd1234').success).toBe(true);
  });
});
