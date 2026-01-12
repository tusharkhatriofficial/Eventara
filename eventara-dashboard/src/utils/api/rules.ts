import {
  CreateRuleRequest,
  RuleResponse,
  UpdateRuleRequest,
  RuleTestResult,
  TestRuleRequest,
} from '../../types/rules';

const BASE = import.meta.env.VITE_API_URL || '';
// When running in Docker Compose we expose backend at http://springboot:8080, but
// for local development Docker Compose sets VITE_API_URL to http://localhost:8080.

async function handleResponse(res: Response) {
  const text = await res.text();
  try {
    const json = text ? JSON.parse(text) : null;
    if (!res.ok) throw json || { status: res.status, message: res.statusText };
    return json;
  } catch (e) {
    if (!res.ok) throw { status: res.status, message: res.statusText };
    return text;
  }
}

export async function listRules(): Promise<RuleResponse[]> {
  const res = await fetch(`${BASE}/api/v1/rules`);
  return handleResponse(res);
}

export async function getRule(id: number): Promise<RuleResponse> {
  const res = await fetch(`${BASE}/api/v1/rules/${id}`);
  return handleResponse(res);
}

export async function createRule(payload: CreateRuleRequest): Promise<RuleResponse> {
  const res = await fetch(`${BASE}/api/v1/rules`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return handleResponse(res);
}

export async function updateRule(id: number, payload: UpdateRuleRequest): Promise<RuleResponse> {
  const res = await fetch(`${BASE}/api/v1/rules/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return handleResponse(res);
}

export async function deleteRule(id: number): Promise<void> {
  const res = await fetch(`${BASE}/api/v1/rules/${id}`, { method: 'DELETE' });
  return handleResponse(res);
}

export async function enableRule(id: number): Promise<RuleResponse> {
  const res = await fetch(`${BASE}/api/v1/rules/${id}/enable`, { method: 'POST' });
  return handleResponse(res);
}

export async function disableRule(id: number): Promise<RuleResponse> {
  const res = await fetch(`${BASE}/api/v1/rules/${id}/disable`, { method: 'POST' });
  return handleResponse(res);
}

export async function archiveRule(id: number): Promise<RuleResponse> {
  const res = await fetch(`${BASE}/api/v1/rules/${id}/archive`, { method: 'POST' });
  return handleResponse(res);
}

export async function testRule(payload: TestRuleRequest): Promise<RuleTestResult> {
  const res = await fetch(`${BASE}/api/v1/rules/test`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return handleResponse(res);
}

export async function testRuleById(id: number): Promise<RuleTestResult> {
  const res = await fetch(`${BASE}/api/v1/rules/test/${id}`, { method: 'POST' });
  return handleResponse(res);
}

export async function getStatistics(): Promise<{ totalRules: number; activeRules: number }> {
  const res = await fetch(`${BASE}/api/v1/rules/statistics`);
  return handleResponse(res);
}
