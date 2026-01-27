export type ChannelType = 'WEBHOOK' | 'EMAIL' | 'SLACK' | 'SMS' | 'PAGERDUTY';

export type NotificationStatus = 'SENT' | 'FAILED' | 'PENDING' | 'RETRYING';

export interface NotificationChannel {
    id: number;
    channelType: ChannelType;
    name: string;
    description?: string;
    enabled: boolean;
    config: Record<string, any>;
    rateLimitPerMinute?: number;
    rateLimitPerHour?: number;
    lastUsedAt?: string;
    totalSent?: number;
    totalFailed?: number;
    createdAt?: string;
    updatedAt?: string;
    createdBy?: string;
}

export interface CreateNotificationChannelRequest {
    channelType: ChannelType;
    name: string;
    description?: string;
    enabled?: boolean;
    config: Record<string, any>;
    rateLimitPerMinute?: number;
    rateLimitPerHour?: number;
    createdBy?: string;
}

export interface UpdateNotificationChannelRequest {
    name?: string;
    description?: string;
    enabled?: boolean;
    config?: Record<string, any>;
    rateLimitPerMinute?: number;
    rateLimitPerHour?: number;
}

export interface NotificationLog {
    id: number;
    alertId?: number;
    channelId?: number;
    channelType: ChannelType;
    recipient: string;
    status: NotificationStatus;
    sentAt?: string;
    subject?: string;
    message: string;
    responseCode?: number;
    responseBody?: string;
    errorMessage?: string;
    retryCount?: number;
    deliveryTimeMs?: number;
    createdAt: string;
}

export interface NotificationStats {
    total: number;
    totalSent: number;
    totalFailed: number;
    successRate: number;
    periodHours: number;
}

export interface NotificationResult {
    id?: number;
    alertId?: number;
    channelId?: number;
    channelName?: string;
    channelType?: string;
    recipient?: string;
    status: NotificationStatus;
    message?: string;
    sentAt?: string;
    responseCode?: number;
    responseBody?: string;
    retryCount?: number;
    deliveryTimeMs?: number;
    errorMessage?: string;
}
