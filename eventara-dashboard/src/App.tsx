import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { useWebSocketMetrics } from './hooks/useWebSocket';
import { DashboardLayout } from './components/layout/DashboardLayout';
import { Overview } from './pages/Overview';
import { ComingSoon } from './pages/ComingSoon';
import './utils/chartConfig';
import './App.css';
import { RealTimeMonitoring } from './pages/RealTimeMonitoring';
import { EventAnalytics } from './pages/EventAnalytics';
import { SourceAnalytics } from './pages/SourceAnalytics';
import { UserAnalytics } from './pages/UserAnalytics';
import { PerformanceMetrics } from './pages/PerformanceMetrics';
import { ErrorAnalysis } from './pages/ErrorAnalysis';
import { AlertsAndAnomalies } from './pages/AlertsAndAnomalies';

function App() {
  const { metrics, connectionState, reconnect } = useWebSocketMetrics();

  return (
    <BrowserRouter>
      <Routes>
        <Route
          element={
            <DashboardLayout 
              connectionState={connectionState} 
              onReconnect={reconnect}
            />
          }
        >
          <Route index element={<Overview metrics={metrics} />} />
          <Route path="/monitoring" element={<RealTimeMonitoring metrics={metrics}/>} />
          <Route path="/events" element={<EventAnalytics metrics={metrics}/>} />
          <Route path="/sources" element={<SourceAnalytics metrics={metrics} />} />
          <Route path="/users" element={<UserAnalytics metrics={metrics} />} />
          <Route path="/performance" element={<PerformanceMetrics metrics={metrics} />} />
          <Route path="/errors" element={<ErrorAnalysis metrics={metrics} />} />
          <Route path="/alerts" element={<AlertsAndAnomalies metrics={metrics} />} />
          <Route path="/reports" element={<ComingSoon pageName="Reports" />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;