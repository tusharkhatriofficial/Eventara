import {
    NotificationChannel,
    CreateNotificationChannelRequest,
    UpdateNotificationChannelRequest,
    NotificationLog,
    NotificationStats,
    NotificationResult,
} from '../../types/notifications';

const BASE = import.meta.env.VITE_API_URL || '';

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

// Channel APIs
export async function listChannels(params?: {
    type?: string;
    enabled?: boolean;
}): Promise<NotificationChannel[]> {
    const queryParams = new URLSearchParams();
    if (params?.type) queryParams.append('type', params.type);
    if (params?.enabled !== undefined) queryParams.append('enabled', String(params.enabled));

    const query = queryParams.toString();
    const url = `${BASE}/api/v1/notifications/channels${query ? `?${query}` : ''}`;
    const res = await fetch(url);
    return handleResponse(res);
}

export async function getChannel(id: number): Promise<NotificationChannel> {
    const res = await fetch(`${BASE}/api/v1/notifications/channels/${id}`);
    return handleResponse(res);
}

export async function createChannel(
    payload: CreateNotificationChannelRequest
): Promise<NotificationChannel> {
    const res = await fetch(`${BASE}/api/v1/notifications/channels`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    });
    return handleResponse(res);
}

export async function updateChannel(
    id: number,
    payload: UpdateNotificationChannelRequest
): Promise<NotificationChannel> {
    const res = await fetch(`${BASE}/api/v1/notifications/channels/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    });
    return handleResponse(res);
}

export async function deleteChannel(id: number): Promise<void> {
    const res = await fetch(`${BASE}/api/v1/notifications/channels/${id}`, {
        method: 'DELETE',
    });
    return handleResponse(res);
}

export async function testChannel(id: number): Promise<NotificationResult> {
    const res = await fetch(`${BASE}/api/v1/notifications/channels/${id}/test`, {
        method: 'POST',
    });
    return handleResponse(res);
}

export async function getChannelStats(): Promise<{
    total: number;
    enabled: number;
    webhooks: number;
    emails: number;
    slack: number;
}> {
    const res = await fetch(`${BASE}/api/v1/notifications/channels/stats`);
    return handleResponse(res);
}

// Notification Logs APIs
export async function listNotificationLogs(params?: {
    channelId?: number;
    alertId?: number;
    status?: string;
    page?: number;
    size?: number;
}): Promise<{ content: NotificationLog[]; totalElements: number; totalPages: number }> {
    const queryParams = new URLSearchParams();
    if (params?.channelId) queryParams.append('channelId', String(params.channelId));
    if (params?.alertId) queryParams.append('alertId', String(params.alertId));
    if (params?.status) queryParams.append('status', params.status);
    if (params?.page !== undefined) queryParams.append('page', String(params.page));
    if (params?.size !== undefined) queryParams.append('size', String(params.size));

    const query = queryParams.toString();
    const url = `${BASE}/api/v1/notifications/logs${query ? `?${query}` : ''}`;
    const res = await fetch(url);
    return handleResponse(res);
}

export async function getNotificationLog(id: number): Promise<NotificationLog> {
    const res = await fetch(`${BASE}/api/v1/notifications/logs/${id}`);
    return handleResponse(res);
}

export async function getNotificationStats(hours?: number): Promise<NotificationStats> {
    const queryParams = new URLSearchParams();
    if (hours) queryParams.append('hours', String(hours));

    const query = queryParams.toString();
    const url = `${BASE}/api/v1/notifications/logs/stats${query ? `?${query}` : ''}`;
    const res = await fetch(url);
    return handleResponse(res);
}

export async function getFailedNotifications(limit?: number): Promise<NotificationLog[]> {
    const queryParams = new URLSearchParams();
    if (limit) queryParams.append('limit', String(limit));

    const query = queryParams.toString();
    const url = `${BASE}/api/v1/notifications/logs/failed${query ? `?${query}` : ''}`;
    const res = await fetch(url);
    return handleResponse(res);
}
