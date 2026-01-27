export type RuleType =
  | 'THRESHOLD'
  | 'PATTERN'
  | 'ANOMALY'
  | 'CEP';

export type RuleStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED' | 'DRAFT';

export type Condition =
  | 'GREATER_THAN'
  | 'LESS_THAN'
  | 'EQUALS'
  | 'GREATER_THAN_OR_EQUAL'
  | 'LESS_THAN_OR_EQUAL'
  | 'NOT_EQUALS'
  | 'BETWEEN'
  | 'NOT_BETWEEN';

export type MetricType =
  | 'ERROR_RATE'
  | 'TOTAL_ERRORS'
  | 'AVG_LATENCY'
  | 'P50_LATENCY'
  | 'P95_LATENCY'
  | 'P99_LATENCY'
  | 'MAX_LATENCY'
  | 'MIN_LATENCY'
  | 'EVENTS_PER_SECOND'
  | 'EVENTS_PER_MINUTE'
  | 'EVENTS_PER_HOUR'
  | 'EVENTS_PER_DAY'
  | 'PEAK_THROUGHPUT'
  | 'AVG_THROUGHPUT_1H'
  | 'AVG_THROUGHPUT_24H'
  | 'EVENTS_LAST_1_MINUTE'
  | 'EVENTS_LAST_5_MINUTES'
  | 'EVENTS_LAST_15_MINUTES'
  | 'EVENTS_LAST_1_HOUR'
  | 'EVENTS_LAST_24_HOURS'
  | 'TOTAL_EVENTS'
  | 'UNIQUE_SOURCES'
  | 'UNIQUE_EVENT_TYPES'
  | 'UNIQUE_USERS'
  | 'SYSTEM_HEALTH'
  | 'ACTIVE_USERS_LAST_1_HOUR'
  | 'ACTIVE_USERS_LAST_24_HOURS'
  | 'TOTAL_UNIQUE_USERS';

export interface CreateRuleRequest {
  name: string;
  description?: string;
  ruleType: RuleType;
  ruleConfig: Record<string, any>;
  severity: string; // use string to align with backend AlertSeverity
  priority?: number;
  notificationChannels?: string[];
  notificationConfig?: Record<string, any>;
  suppressionWindowMinutes?: number;
  maxAlertsPerHour?: number;
  createdBy?: string;
}

export interface UpdateRuleRequest {
  name?: string;
  description?: string;
  ruleType?: RuleType;
  ruleConfig?: Record<string, any>;
  severity?: string;
  priority?: number;
  notificationChannels?: string[];
  notificationConfig?: Record<string, any>;
  suppressionWindowMinutes?: number;
  maxAlertsPerHour?: number;
  createdBy?: string;
}

export interface RuleResponse {
  id: number;
  name: string;
  description?: string;
  ruleType: RuleType;
  status: RuleStatus;
  ruleConfig: Record<string, any>;
  severity: string;
  priority?: number;
  notificationChannels?: string[];
  notificationConfig?: Record<string, any>;
  suppressionWindowMinutes?: number;
  maxAlertsPerHour?: number;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  lastTriggeredAt?: string;
  triggerCount?: number;
  version?: number;
}

export interface TestRuleRequest {
  ruleType: RuleType;
  ruleConfig: Record<string, any>;
  severity: string;
  priority?: number;
  name?: string;
}

export interface RuleTestResult {
  success: boolean;
  message?: string;
  generatedDrl?: string;
  errors?: string[];
  warnings?: string[];
}
