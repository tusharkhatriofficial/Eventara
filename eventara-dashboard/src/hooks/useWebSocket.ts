import { useEffect, useState, useCallback, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Client, IMessage } from '@stomp/stompjs';
import { ComprehensiveMetrics, ConnectionState, UseWebSocketReturn } from '../types';

interface UseWebSocketMetricsReturn extends UseWebSocketReturn {
  metrics: ComprehensiveMetrics | null;
}

// Derive WebSocket URL from the current page location so it works on any deployment
const getDefaultWsUrl = (): string => {
  if (import.meta.env.VITE_API_URL) return `${import.meta.env.VITE_API_URL}/ws`;
  // In production, use the same origin as the page (works for VPS, Coolify, etc.)
  if (typeof window !== 'undefined') return `${window.location.origin}/ws`;
  return 'http://localhost:8080/ws';
};

export const useWebSocketMetrics = (url: string = getDefaultWsUrl()): UseWebSocketMetricsReturn => {
  const [metrics, setMetrics] = useState<ComprehensiveMetrics | null>(null);
  const [connectionState, setConnectionState] = useState<ConnectionState>(ConnectionState.CONNECTING);
  const [error, setError] = useState<Error | null>(null);

  const clientRef = useRef<Client | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const reconnectAttemptsRef = useRef(0);

  const MAX_RECONNECT_ATTEMPTS = 10;
  const RECONNECT_INTERVAL = 3000; // 3 seconds

  const connect = useCallback(() => {
    try {
      setConnectionState(ConnectionState.CONNECTING);
      setError(null);

      // Create SockJS socket
      const socket = new SockJS(url);

      // Create STOMP client
      const stompClient = new Client({
        webSocketFactory: () => socket as any,
        debug: (str) => {
          console.log('[STOMP Debug]', str);
        },
        reconnectDelay: RECONNECT_INTERVAL,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      // Connection established
      stompClient.onConnect = () => {
        console.log('WebSocket Connected');
        setConnectionState(ConnectionState.CONNECTED);
        reconnectAttemptsRef.current = 0;

        // Subscribe to metrics topic
        stompClient.subscribe('/topic/metrics', (message: IMessage) => {
          try {
            const metricsData: ComprehensiveMetrics = JSON.parse(message.body);
            setMetrics(metricsData);
            console.log('Metrics received:', metricsData.summary.totalEvents);
          } catch (err) {
            console.error('Error parsing metrics:', err);
          }
        });

        // Request initial metrics
        stompClient.publish({
          destination: '/app/subscribe',
          body: JSON.stringify({ action: 'subscribe' })
        });
      };

      // Connection error
      stompClient.onStompError = (frame) => {
        console.error('STOMP error:', frame.headers['message']);
        setConnectionState(ConnectionState.ERROR);
        setError(new Error(frame.headers['message'] || 'STOMP connection error'));
      };

      // WebSocket error
      stompClient.onWebSocketError = (event) => {
        console.error('WebSocket error:', event);
        setConnectionState(ConnectionState.ERROR);
        setError(new Error('WebSocket connection error'));
      };

      // Connection closed
      stompClient.onWebSocketClose = () => {
        console.log('🔌 WebSocket connection closed');
        setConnectionState(ConnectionState.DISCONNECTED);

        // Attempt reconnection
        if (reconnectAttemptsRef.current < MAX_RECONNECT_ATTEMPTS) {
          reconnectAttemptsRef.current++;
          console.log(`Reconnecting... (Attempt ${reconnectAttemptsRef.current}/${MAX_RECONNECT_ATTEMPTS})`);
          setConnectionState(ConnectionState.RECONNECTING);

          reconnectTimeoutRef.current = setTimeout(() => {
            connect();
          }, RECONNECT_INTERVAL);
        } else {
          console.error('Max reconnection attempts reached');
          setError(new Error('Failed to reconnect after multiple attempts'));
        }
      };

      // Activate the client
      stompClient.activate();
      clientRef.current = stompClient;

    } catch (err) {
      console.error('Connection error:', err);
      setConnectionState(ConnectionState.ERROR);
      setError(err instanceof Error ? err : new Error('Unknown connection error'));
    }
  }, [url]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }

    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }

    setConnectionState(ConnectionState.DISCONNECTED);
    console.log('🔌 Disconnected');
  }, []);

  const reconnect = useCallback(() => {
    disconnect();
    reconnectAttemptsRef.current = 0;
    setTimeout(() => connect(), 500);
  }, [connect, disconnect]);

  // Connect on mount
  useEffect(() => {
    connect();

    // Cleanup on unmount
    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return {
    metrics,
    isConnected: connectionState === ConnectionState.CONNECTED,
    connectionState,
    error,
    reconnect,
    disconnect
  };
};
