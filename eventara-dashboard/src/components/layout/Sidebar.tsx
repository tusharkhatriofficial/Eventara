import { Link, useLocation } from 'react-router-dom';

interface SidebarProps {
  isOpen: boolean;
  isCollapsed: boolean;
  onClose: () => void;
  onToggleCollapse: () => void;
}

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

const navigationItems: NavItem[] = [
  { path: '/', label: 'Overview', icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' },
  { path: '/monitoring', label: 'Real-Time', icon: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z' },
  { path: '/events', label: 'Events', icon: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z' },
  { path: '/sources', label: 'Sources', icon: 'M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01' },
  { path: '/users', label: 'Users', icon: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z' },
  { path: '/performance', label: 'Performance', icon: 'M13 10V3L4 14h7v7l9-11h-7z' },
  { path: '/errors', label: 'Errors', icon: 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z' },
  { path: '/alerts', label: 'Alerts', icon: 'M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9' },
  { path: '/reports', label: 'Reports', icon: 'M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z' },
];

export const Sidebar: React.FC<SidebarProps> = ({
  isOpen,
  isCollapsed,
  onClose,
  onToggleCollapse
}) => {
  const location = useLocation();

  const isActive = (path: string) => {
    if (path === '/') return location.pathname === path;
    return location.pathname.startsWith(path);
  };

  return (
    <aside className={`
      fixed inset-y-0 left-0 z-50
      flex flex-col
      bg-slate-900 text-white
      transition-all duration-300 ease-smooth
      ${isCollapsed ? 'w-20' : 'w-72'}
      ${isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
    `}>
      {/* Logo */}
      <div className={`
        flex items-center gap-3 h-[73px] px-6
        border-b border-slate-800
        ${isCollapsed ? 'justify-center px-4' : ''}
      `}>
        <div className="relative group">
          {/* Animated glow effect */}
          <div className="absolute inset-0 bg-primary-600 rounded-2xl blur-md opacity-30 group-hover:opacity-50 transition-all duration-500 animate-pulse"></div>

          {/* Logo container with premium gradient */}
          <div className="relative w-10 h-10 rounded-2xl bg-gradient-to-br from-primary-400 via-primary-500 to-primary-700 flex items-center justify-center shadow-xl shadow-primary-500/30 flex-shrink-0 border border-white/20 overflow-hidden">
            {/* Subtle shine effect */}
            <div className="absolute inset-0 bg-gradient-to-tr from-transparent via-white/10 to-transparent transform -translate-x-full group-hover:translate-x-full transition-transform duration-1000"></div>

            {/* Custom Eventara Logo - Event Stream Network */}
            <svg className="w-6 h-6 text-white relative z-10 transform transition-all duration-500 ease-out group-hover:scale-110" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              {/* Central node */}
              <circle cx="12" cy="12" r="2" fill="currentColor" />

              {/* Outer nodes - representing event sources */}
              <circle cx="6" cy="6" r="1.5" fill="currentColor" />
              <circle cx="18" cy="6" r="1.5" fill="currentColor" />
              <circle cx="6" cy="18" r="1.5" fill="currentColor" />
              <circle cx="18" cy="18" r="1.5" fill="currentColor" />

              {/* Connection lines - representing event streams */}
              <path d="M7.5 7.5L10 10" strokeWidth="1.5" opacity="0.8" />
              <path d="M16.5 7.5L14 10" strokeWidth="1.5" opacity="0.8" />
              <path d="M7.5 16.5L10 14" strokeWidth="1.5" opacity="0.8" />
              <path d="M16.5 16.5L14 14" strokeWidth="1.5" opacity="0.8" />

              {/* Accent ring around center */}
              <circle cx="12" cy="12" r="4.5" strokeWidth="1" opacity="0.4" fill="none" className="animate-ping" style={{ animationDuration: '3s' }} />
            </svg>
          </div>
        </div>
        {!isCollapsed && (
          <div className="min-w-0 flex-1">
            <h1 className="text-lg font-bold text-white tracking-tight truncate">Eventara</h1>
            <p className="text-xs text-slate-400 truncate">Event Analytics Platform</p>
          </div>
        )}

        {/* Close button for mobile */}
        <button
          onClick={onClose}
          className="lg:hidden p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      {/* Navigation */}
      <nav className="flex-1 p-3 space-y-1 overflow-y-auto scrollbar-thin">
        {navigationItems.map((item) => (
          <Link
            key={item.path}
            to={item.path}
            onClick={onClose}
            className={`
              group flex items-center gap-3 px-3 py-2.5 rounded-xl
              transition-all duration-200
              ${isActive(item.path)
                ? 'bg-gradient-to-r from-primary-600 to-primary-700 text-white shadow-lg shadow-primary-500/25'
                : 'text-slate-400 hover:bg-slate-800 hover:text-white'
              }
              ${isCollapsed ? 'justify-center' : ''}
            `}
            title={isCollapsed ? item.label : undefined}
          >
            <svg
              className={`w-5 h-5 flex-shrink-0 transition-transform duration-200 ${!isActive(item.path) ? 'group-hover:scale-110' : ''
                }`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d={item.icon}
              />
            </svg>
            {!isCollapsed && (
              <span className="text-sm font-medium truncate">{item.label}</span>
            )}
          </Link>
        ))}
      </nav>

      {/* Collapse toggle (desktop only) */}
      <div className="hidden lg:block p-3 border-t border-slate-800">
        <button
          onClick={onToggleCollapse}
          className={`
            w-full flex items-center gap-3 px-3 py-2.5 rounded-xl
            text-slate-400 hover:bg-slate-800 hover:text-white
            transition-all duration-200
            ${isCollapsed ? 'justify-center' : ''}
          `}
        >
          <svg
            className={`w-5 h-5 transition-transform duration-300 ${isCollapsed ? 'rotate-180' : ''}`}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
          </svg>
          {!isCollapsed && <span className="text-sm font-medium">Collapse</span>}
        </button>
      </div>

      {/* Footer */}
      <div className={`p-3 border-t border-slate-800 ${isCollapsed ? 'px-2' : ''}`}>
        <div className={`
          flex items-center gap-3 p-3 bg-slate-800/50 rounded-xl
          ${isCollapsed ? 'justify-center p-2' : ''}
        `}>
          <div className="w-2 h-2 rounded-full bg-success-500 animate-pulse shadow-lg shadow-success-500/50 flex-shrink-0"></div>
          {!isCollapsed && (
            <div className="min-w-0 flex-1">
              <p className="text-xs font-medium text-slate-300 truncate">System Online</p>
              <p className="text-xs text-slate-500 truncate">v1.0.0</p>
            </div>
          )}
        </div>
      </div>
    </aside>
  );
};
