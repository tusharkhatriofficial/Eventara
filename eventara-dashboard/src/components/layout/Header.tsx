import { ConnectionStatus } from './ConnectionStatus';
import { ConnectionState } from '../../types';

interface HeaderProps {
  connectionState: ConnectionState;
  onReconnect: () => void;
}

export const Header: React.FC<HeaderProps> = ({ connectionState, onReconnect }) => {
  return (
    <header className="bg-white/80 backdrop-blur-md border-b border-dark-100 px-8 py-4 shadow-sm sticky top-0 z-10">
      <div className="flex items-center justify-between">
        {/* Left side - Date and breadcrumb */}
        <div className="flex-1 flex items-center gap-6">
          <div className="flex items-center gap-2 text-sm text-dark-600">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            <span className="font-medium">
              {new Date().toLocaleDateString('en-US', { 
                weekday: 'short',
                month: 'short',
                day: 'numeric',
                year: 'numeric' 
              })}
            </span>
          </div>
        </div>

        {/* Right side - Connection status and actions */}
        <div className="flex items-center gap-4">
          <ConnectionStatus 
            connectionState={connectionState} 
            onReconnect={onReconnect}
          />
          
          {/* Search button */}
          <button className="p-2.5 text-dark-500 hover:text-dark-700 hover:bg-dark-50 rounded-xl transition-all duration-200">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </button>

          {/* Notifications button with badge */}
          <button className="relative p-2.5 text-dark-500 hover:text-dark-700 hover:bg-dark-50 rounded-xl transition-all duration-200">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
            <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full ring-2 ring-white"></span>
          </button>

          {/* Settings button */}
          <button className="p-2.5 text-dark-500 hover:text-dark-700 hover:bg-dark-50 rounded-xl transition-all duration-200">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </button>

          {/* User profile */}
          <div className="flex items-center gap-3 pl-4 ml-4 border-l border-dark-200">
            <div className="text-right hidden sm:block">
              <p className="text-sm font-semibold text-dark-900">Admin User</p>
              <p className="text-xs text-dark-500">admin@eventara</p>
            </div>
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center text-white font-semibold shadow-lg shadow-primary-500/30">
              AU
            </div>
          </div>
        </div>
      </div>
    </header>
  );
};
