import { useState } from 'react';
import {
  AppShell,
  Group,
  Select,
  SimpleGrid,
  Skeleton,
  Stack,
  Text,
  Title,
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useQuery } from '@tanstack/react-query';
import { AppHeader } from '../components/layout/AppHeader';
import { FilterPanel, EMPTY_FILTERS } from '../components/filters/FilterPanel';
import type { FilterState } from '../components/filters/FilterPanel';
import { ProductCard } from '../components/products/ProductCard';
import productsApi from '../services/productsApi';
import type { Product } from '../services/productsApi';

const SORT_OPTIONS = [
  { value: 'updatedAt,desc', label: 'Recently updated' },
  { value: 'name,asc', label: 'Name (A–Z)' },
  { value: 'name,desc', label: 'Name (Z–A)' },
];

const MAX_RESULTS = 150;

function applyFilters(products: Product[], filters: FilterState): Product[] {
  return products.filter((p) => {
    if (filters.roastLevel.length > 0 && !filters.roastLevel.includes(p.roastLevel ?? ''))
      return false;
    if (filters.process.length > 0 && !filters.process.includes(p.process ?? ''))
      return false;
    if (filters.productType.length > 0 && !filters.productType.includes(p.productType ?? ''))
      return false;
    if (
      filters.availability.length > 0 &&
      !filters.availability.includes(p.availabilityType ?? '')
    )
      return false;
    if (filters.decafOnly && !p.decaf) return false;
    return true;
  });
}

export function BrowsePage() {
  const [navOpened, { toggle: toggleNav, close: closeNav }] = useDisclosure(false);
  const [filters, setFilters] = useState<FilterState>(EMPTY_FILTERS);
  const [sortBy, setSortBy] = useState('updatedAt,desc');

  const { data, isLoading } = useQuery({
    queryKey: ['products', { activeOnly: true, size: MAX_RESULTS, sort: sortBy }],
    queryFn: () => productsApi.list({ activeOnly: true, size: MAX_RESULTS, sort: sortBy }),
  });

  const allProducts = data?.content ?? [];
  const filtered = applyFilters(allProducts, filters);
  const total = data?.totalElements ?? 0;

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{ width: 280, breakpoint: 'sm', collapsed: { mobile: !navOpened } }}
      padding="md"
    >
      <AppHeader navOpened={navOpened} onNavToggle={toggleNav} />

      <AppShell.Navbar>
        <FilterPanel
          filters={filters}
          onChange={(f) => {
            setFilters(f);
            closeNav();
          }}
        />
      </AppShell.Navbar>

      <AppShell.Main>
        <Stack gap="md">
          <Group justify="space-between" align="center">
            <Title order={2}>Browse</Title>
            <Select
              size="sm"
              value={sortBy}
              onChange={(v) => setSortBy(v ?? 'updatedAt,desc')}
              data={SORT_OPTIONS}
              w={180}
              allowDeselect={false}
            />
          </Group>

          {!isLoading && total > MAX_RESULTS && (
            <Text size="sm" c="dimmed">
              Showing {MAX_RESULTS} of {total} — refine your filters to see more.
            </Text>
          )}

          {isLoading ? (
            <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
              {Array.from({ length: 6 }).map((_, i) => (
                <Skeleton key={i} height={160} radius="md" />
              ))}
            </SimpleGrid>
          ) : filtered.length === 0 ? (
            <Stack align="center" gap="xs" py="xl">
              <Text size="lg" c="dimmed">No products match your filters.</Text>
              <Text size="sm" c="dimmed">Try clearing some filters to see more results.</Text>
            </Stack>
          ) : (
            <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
              {filtered.map((p) => (
                <ProductCard key={p.id} product={p} />
              ))}
            </SimpleGrid>
          )}
        </Stack>
      </AppShell.Main>
    </AppShell>
  );
}
