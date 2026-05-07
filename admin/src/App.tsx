import { AdminShell } from './components/layout/AdminShell';
import { RoastersPage } from './pages/RoastersPage';

export default function App() {
  return (
    <AdminShell>
      <RoastersPage />
    </AdminShell>
  );
}