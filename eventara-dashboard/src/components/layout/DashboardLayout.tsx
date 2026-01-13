import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { ConnectionState } from '../../types';

interface DashboardLayoutProps {
  connectionState: ConnectionState;
  onReconnect: () => void;
}

export const DashboardLayout: React.FC<DashboardLayoutProps> = ({ 
  connectionState, 
  onReconnect 
}) => {
  return (
    <div className="flex min-h-screen bg-gradient-to-br from-dark-50 via-white to-primary-50/30">
      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0">
        <Header connectionState={connectionState} onReconnect={onReconnect} />
        <main className="flex-1 p-8 overflow-auto scrollbar-thin">
          <div className="animate-fade-in">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};
