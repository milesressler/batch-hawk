import { Route, Routes } from 'react-router-dom';
import { AdminShell } from './components/layout/AdminShell';
import { DashboardPage } from './pages/DashboardPage';
import { RoastersPage } from './pages/RoastersPage';
import { RoasterProductsPage } from './pages/RoasterProductsPage';
import { AgentRunsPage } from './pages/AgentRunsPage';
import { RunDetailPage } from './pages/RunDetailPage';

export default function App() {
  return (
    <AdminShell>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/roasters" element={<RoastersPage />} />
        <Route path="/roasters/:id/products" element={<RoasterProductsPage />} />
        <Route path="/runs" element={<AgentRunsPage />} />
        <Route path="/runs/:id" element={<RunDetailPage />} />
      </Routes>
    </AdminShell>
  );
}
