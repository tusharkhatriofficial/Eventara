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
          icon: '',
          text: 'Connected',
          bgColor: 'bg-green-100',
          textColor: 'text-green-800',
          dotColor: 'bg-green-500',
          animate: 'animate-pulse'
        };
      case ConnectionState.CONNECTING:
        return {
          icon: '',
          text: 'Connecting...',
          bgColor: 'bg-blue-100',
          textColor: 'text-blue-800',
          dotColor: 'bg-blue-500',
          animate: 'animate-pulse'
        };
      case ConnectionState.RECONNECTING:
        return {
          icon: '',
          text: 'Reconnecting...',
          bgColor: 'bg-yellow-100',
          textColor: 'text-yellow-800',
          dotColor: 'bg-yellow-500',
          animate: 'animate-spin'
        };
      case ConnectionState.DISCONNECTED:
        return {
          icon: '',
          text: 'Disconnected',
          bgColor: 'bg-gray-100',
          textColor: 'text-gray-800',
          dotColor: 'bg-gray-500',
          animate: ''
        };
      case ConnectionState.ERROR:
        return {
          icon: '',
          text: 'Connection Error',
          bgColor: 'bg-red-100',
          textColor: 'text-red-800',
          dotColor: 'bg-red-500',
          animate: ''
        };
      default:
        return {
          icon: '❓',
          text: 'Unknown',
          bgColor: 'bg-gray-100',
          textColor: 'text-gray-800',
          dotColor: 'bg-gray-500',
          animate: ''
        };
    }
  };

  const config = getStatusConfig();
  const showReconnect = [ConnectionState.DISCONNECTED, ConnectionState.ERROR].includes(connectionState);

  return (
    <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full ${config.bgColor}`}>
      {/* Animated dot */}
      <div className="relative flex h-2 w-2">
        <span className={`absolute inline-flex h-full w-full rounded-full ${config.dotColor} opacity-75 ${config.animate}`}></span>
        <span className={`relative inline-flex rounded-full h-2 w-2 ${config.dotColor}`}></span>
      </div>
      
      {/* Status text */}
      <span className={`text-xs font-medium ${config.textColor}`}>
        {config.icon} {config.text}
      </span>

      {/* Reconnect button */}
      {showReconnect && (
        <button
          onClick={onReconnect}
          className="ml-1 text-xs font-medium underline hover:no-underline"
        >
          Retry
        </button>
      )}
    </div>
  );
};
