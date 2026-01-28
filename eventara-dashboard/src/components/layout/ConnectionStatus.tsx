import { ConnectionState } from '../../types';

interface ConnectionStatusProps {
  connectionState: ConnectionState;
  onReconnect: () => void;
}

export const ConnectionStatus: React.FC<ConnectionStatusProps> = ({
  connectionState,
  onReconnect
}) => {
  const getStatusConfig = () => {
    switch (connectionState) {
      case ConnectionState.CONNECTED:
        return {
          color: 'bg-success-500',
          pulseColor: 'shadow-success-500/50',
          text: 'Connected',
          textColor: 'text-success-700',
          bgColor: 'bg-success-50',
          showReconnect: false,
        };
      case ConnectionState.CONNECTING:
        return {
          color: 'bg-warning-500',
          pulseColor: 'shadow-warning-500/50',
          text: 'Connecting',
          textColor: 'text-warning-700',
          bgColor: 'bg-warning-50',
          showReconnect: false,
        };
      case ConnectionState.RECONNECTING:
        return {
          color: 'bg-warning-500',
          pulseColor: 'shadow-warning-500/50',
          text: 'Reconnecting',
          textColor: 'text-warning-700',
          bgColor: 'bg-warning-50',
          showReconnect: false,
        };
      case ConnectionState.DISCONNECTED:
      case ConnectionState.ERROR:
        return {
          color: 'bg-error-500',
          pulseColor: 'shadow-error-500/50',
          text: 'Disconnected',
          textColor: 'text-error-700',
          bgColor: 'bg-error-50',
          showReconnect: true,
        };
      default:
        return {
          color: 'bg-slate-400',
          pulseColor: 'shadow-slate-400/50',
          text: 'Unknown',
          textColor: 'text-slate-600',
          bgColor: 'bg-slate-50',
          showReconnect: true,
        };
    }
  };

  const config = getStatusConfig();
  const isConnecting = connectionState === ConnectionState.CONNECTING ||
    connectionState === ConnectionState.RECONNECTING;

  return (
    <div className={`
      inline-flex items-center gap-2 px-3 py-1.5 rounded-full
      ${config.bgColor} ring-1 ring-inset ring-current/10
      transition-all duration-200
    `}>
      <div className="relative flex items-center justify-center">
        <span className={`
          w-2 h-2 rounded-full ${config.color}
          ${isConnecting ? 'animate-pulse' : ''}
          shadow-lg ${config.pulseColor}
        `}></span>
        {connectionState === ConnectionState.CONNECTED && (
          <span className={`
            absolute w-2 h-2 rounded-full ${config.color}
            animate-ping opacity-75
          `}></span>
        )}
      </div>

      <span className={`text-xs font-medium ${config.textColor} hidden sm:inline`}>
        {config.text}
      </span>

      {config.showReconnect && (
        <button
          onClick={onReconnect}
          className={`
            text-xs font-medium ${config.textColor} 
            hover:underline focus:outline-none
            hidden sm:inline
          `}
        >
          Retry
        </button>
      )}
    </div>
  );
};
