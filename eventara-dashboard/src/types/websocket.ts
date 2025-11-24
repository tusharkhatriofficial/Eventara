// WebSocket connection states
export enum ConnectionState {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  ERROR = 'ERROR',
  RECONNECTING = 'RECONNECTING'
}

// WebSocket message types
export interface WebSocketMessage<T = any> {
  type: string;
  payload: T;
  timestamp: string;
}

// WebSocket configuration
export interface WebSocketConfig {
  url: string;
  reconnectInterval?: number;
  maxReconnectAttempts?: number;
  debug?: boolean;
}

// WebSocket hook return type
export interface UseWebSocketReturn {
  isConnected: boolean;
  connectionState: ConnectionState;
  error: Error | null;
  reconnect: () => void;
  disconnect: () => void;
}
