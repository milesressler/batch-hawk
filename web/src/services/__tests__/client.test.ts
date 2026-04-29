import { describe, it, expect, vi, beforeEach } from 'vitest';
import { setTokenGetter } from '../client';

describe('setTokenGetter', () => {
  beforeEach(() => {
    setTokenGetter(null as never);
  });

  it('accepts a token getter function', () => {
    const getter = vi.fn().mockResolvedValue('tok');
    expect(() => setTokenGetter(getter)).not.toThrow();
  });
});
