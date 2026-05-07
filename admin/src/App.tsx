import { Route, Routes } from 'react-router-dom';
import { AdminShell } from './components/layout/AdminShell';
import { RoastersPage } from './pages/RoastersPage';
import { AgentRunsPage } from './pages/AgentRunsPage';

export default function App() {
  return (
    <AdminShell>
      <Routes>
        <Route path="/" element={<RoastersPage />} />
        <Route path="/runs" element={<AgentRunsPage />} />
      </Routes>
    </AdminShell>
  );
}
