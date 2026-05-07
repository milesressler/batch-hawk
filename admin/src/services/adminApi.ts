const BASE = import.meta.env.VITE_API_BASE_URL ?? '';

export type AgentRunStatus = 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
export type ModerationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type IntegrationType = 'SHOPIFY' | 'WOO_COMMERCE' | 'SQUARE' | 'SQUARESPACE' | 'CUSTOM' | 'UNKNOWN';

export interface AdminRoasterRequest {
  name?: string;
  websiteUrl?: string;
  emailListUrl?: string;
  integrationType?: IntegrationType;
  moderationStatus?: ModerationStatus;
  active?: boolean;
}

export interface AdminRoaster {
  id: string;
  name: string;
  websiteUrl: string | null;
  emailListUrl: string | null;
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

export async function createRoaster(getToken: () => Promise<string>, data: AdminRoasterRequest): Promise<AdminRoaster> {
  const res = await authFetch(getToken, '/api/admin/roasters', { method: 'POST', body: JSON.stringify(data) });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json() as Promise<AdminRoaster>;
}

export async function updateRoaster(getToken: () => Promise<string>, id: string, data: AdminRoasterRequest): Promise<AdminRoaster> {
  const res = await authFetch(getToken, `/api/admin/roasters/${id}`, { method: 'PATCH', body: JSON.stringify(data) });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json() as Promise<AdminRoaster>;
}

export interface AgentRun {
  id: number;
  roasterId: string;
  roasterName: string;
  status: AgentRunStatus;
  startedAt: string | null;
  completedAt: string | null;
  inputTokens: number | null;
  outputTokens: number | null;
  feedbackNotes: string | null;
  checkoutNotes: string | null;
  fieldsFound: string[] | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export async function listAgentRuns(getToken: () => Promise<string>, page = 0, size = 25): Promise<Page<AgentRun>> {
  const res = await authFetch(getToken, `/api/admin/runs?page=${page}&size=${size}`);
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json() as Promise<Page<AgentRun>>;
}

export async function getAgentRun(getToken: () => Promise<string>, id: number): Promise<AgentRun> {
  const res = await authFetch(getToken, `/api/admin/runs/${id}`);
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json() as Promise<AgentRun>;
}

export interface ProductObservation {
  id: string;
  bagSize: number | null;
  bagSizeUnit: string | null;
  bagSizeOz: number | null;
  priceUsd: number | null;
  pricePerOz: number | null;
  inStock: boolean | null;
  observedAt: string;
}

export interface Product {
  id: string;
  roaster: { id: string; name: string; websiteUrl: string | null; active: boolean };
  name: string;
  roastLevel: string | null;
  productType: string | null;
  originCountry: string | null;
  originRegion: string | null;
  process: string | null;
  brewMethods: string[] | null;
  flavorProfile: string[] | null;
  decaf: boolean;
  offersGrinding: boolean;
  availabilityType: string | null;
  description: string | null;
  productUrl: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  variants: ProductObservation[];
}

export async function listRoasterProducts(
  getToken: () => Promise<string>,
  roasterId: string,
  activeOnly: boolean,
  page = 0,
  size = 50,
): Promise<Page<Product>> {
  const params = new URLSearchParams({
    roasterId,
    activeOnly: String(activeOnly),
    page: String(page),
    size: String(size),
    sort: 'name,asc',
  });
  const res = await authFetch(getToken, `/api/products?${params}`);
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json() as Promise<Page<Product>>;
}
