import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ErrorState } from './ErrorState';

/**
 * 조회 실패를 화면에서 "데이터 없음"과 구분해 보여주는 컴포넌트다.
 * 빈 배열을 그대로 렌더하면 서버가 죽은 것과 원래 없는 것을 사용자가 구분할 수 없다.
 */
describe('ErrorState', () => {
  it('기본 문구를 보여준다', () => {
    render(<ErrorState />);
    expect(screen.getByText('데이터를 불러오지 못했습니다.')).toBeInTheDocument();
  });

  it('화면 맥락에 맞는 문구로 덮어쓸 수 있다', () => {
    render(<ErrorState message="초대 목록을 불러오지 못했습니다." />);
    expect(screen.getByText('초대 목록을 불러오지 못했습니다.')).toBeInTheDocument();
    expect(screen.queryByText('데이터를 불러오지 못했습니다.')).not.toBeInTheDocument();
  });

  it('onRetry가 없으면 재시도 버튼도 없다', () => {
    render(<ErrorState />);
    expect(screen.queryByRole('button', { name: '다시 시도' })).not.toBeInTheDocument();
  });

  it('onRetry를 넘기면 버튼이 뜨고 눌리면 호출된다', async () => {
    const onRetry = vi.fn();
    render(<ErrorState onRetry={onRetry} />);

    const button = screen.getByRole('button', { name: '다시 시도' });
    await userEvent.click(button);

    expect(onRetry).toHaveBeenCalledOnce();
  });
});
