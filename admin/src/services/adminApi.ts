const BASE = import.meta.env.VITE_API_BASE_URL ?? '';

export type AgentRunStatus = 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
export type ModerationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type IntegrationType = 'SHOPIFY' | 'UNKNOWN';

export interface AdminRoaster {
  id: string;
  name: string;
  websiteUrl: string | null;
  active: boolean;
  moderationStatus: ModerationStatus;
  integrationType: IntegrationType;
  pendingRefresh: boolean;
  lastRunStartedAt: string | null;
  lastRunCompletedAt: string | null;
  lastRunStatus: AgentRunStatus | null;
}

async function authFetch(getToken: () => Promise<string>, path: string, init?: RequestInit): Promise<Response> {
  const token = await getToken();
  return fetch(`${BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...init?.headers,
    },
  });
}

export async function listRoasters(getToken: () => Promise<string>): Promise<AdminRoaster[]> {
  const res = await authFetch(getToken, '/api/admin/roasters');
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json() as Promise<AdminRoaster[]>;
}

export async function triggerRoaster(getToken: () => Promise<string>, id: string): Promise<void> {
  const res = await authFetch(getToken, `/api/admin/roasters/${id}/trigger`, { method: 'POST' });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
}

export async function deactivateProducts(getToken: () => Promise<string>, id: string): Promise<void> {
  const res = await authFetch(getToken, `/api/admin/roasters/${id}/products/deactivate`, { method: 'POST' });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
}
