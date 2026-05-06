import { Route, Routes } from 'react-router-dom';
import { BrowsePage } from './pages/BrowsePage';
import { ProductDetailPage } from './pages/ProductDetailPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<BrowsePage />} />
      <Route path="/products/:id" element={<ProductDetailPage />} />
    </Routes>
  );
}
