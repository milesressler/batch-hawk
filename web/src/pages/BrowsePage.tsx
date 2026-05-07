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
import { useDebouncedValue, useDisclosure } from '@mantine/hooks';
import { useQuery } from '@tanstack/react-query';
import { AppHeader } from '../components/layout/AppHeader';
import { FilterPanel, EMPTY_FILTERS } from '../components/filters/FilterPanel';
import type { FilterState } from '../components/filters/FilterPanel';
import { ProductCard } from '../components/products/ProductCard';
import productsApi from '../services/productsApi';

const SORT_OPTIONS = [
  { value: 'updatedAt,desc', label: 'Recently updated' },
  { value: 'name,asc', label: 'Name (A–Z)' },
  { value: 'name,desc', label: 'Name (Z–A)' },
];

const PAGE_SIZE = 100;

export function BrowsePage() {
  const [navOpened, { toggle: toggleNav, close: closeNav }] = useDisclosure(false);
  const [filters, setFilters] = useState<FilterState>(EMPTY_FILTERS);
  const [sortBy, setSortBy] = useState('updatedAt,desc');

  const [debouncedKeyword] = useDebouncedValue(filters.keyword, 300);

  const queryParams = {
    activeOnly: true,
    size: PAGE_SIZE,
    sort: sortBy,
    keyword: debouncedKeyword.trim() || undefined,
    roastLevel: filters.roastLevel.length > 0 ? filters.roastLevel : undefined,
    process: filters.process.length > 0 ? filters.process : undefined,
    productType: filters.productType.length > 0 ? filters.productType : undefined,
    availabilityType: filters.availability.length > 0 ? filters.availability : undefined,
    decafOnly: filters.decafOnly || undefined,
  };

  const { data, isLoading } = useQuery({
    queryKey: ['products', queryParams],
    queryFn: () => productsApi.list(queryParams),
  });

  const products = data?.content ?? [];
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

          {!isLoading && total > PAGE_SIZE && (
            <Text size="sm" c="dimmed">
              Showing {PAGE_SIZE} of {total} — refine your filters to see more.
            </Text>
          )}

          {isLoading ? (
            <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
              {Array.from({ length: 6 }).map((_, i) => (
                <Skeleton key={i} height={160} radius="md" />
              ))}
            </SimpleGrid>
          ) : products.length === 0 ? (
            <Stack align="center" gap="xs" py="xl">
              <Text size="lg" c="dimmed">No products match your filters.</Text>
              <Text size="sm" c="dimmed">Try clearing some filters to see more results.</Text>
            </Stack>
          ) : (
            <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
              {products.map((p) => (
                <ProductCard key={p.id} product={p} />
              ))}
            </SimpleGrid>
          )}
        </Stack>
      </AppShell.Main>
    </AppShell>
  );
}
