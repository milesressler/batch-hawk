import { useState } from 'react';
import { Alert, Anchor, Badge, Breadcrumbs, Group, Pagination, Stack, Switch, Table, Text, Title } from '@mantine/core';
import { IconAlertCircle } from '@tabler/icons-react';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider';
import { listRoasterProducts, type Product, type ProductObservation } from '../services/adminApi';

function latestPrice(variants: ProductObservation[]): string {
  if (!variants.length) return '—';
  const sorted = [...variants].sort((a, b) => b.observedAt.localeCompare(a.observedAt));
  const price = sorted[0].priceUsd;
  return price != null ? `$${price.toFixed(2)}` : '—';
}

function originLabel(p: Product): string {
  const parts = [p.originCountry, p.originRegion].filter(Boolean);
  return parts.length ? parts.join(', ') : '—';
}

export function RoasterProductsPage() {
  const { id } = useParams<{ id: string }>();
  const { getToken } = useAuth();
  const [activeOnly, setActiveOnly] = useState(true);
  const [page, setPage] = useState(1);

  const { data, isLoading, error } = useQuery({
    queryKey: ['roaster-products', id, activeOnly, page],
    queryFn: () => listRoasterProducts(getToken, id!, activeOnly, page - 1, 50),
    enabled: !!id,
  });

  const roasterName = data?.content[0]?.roaster.name ?? id;

  if (error) return <Alert icon={<IconAlertCircle />} color="red">{String(error)}</Alert>;

  return (
    <Stack gap="md">
      <Stack gap="xs">
        <Breadcrumbs>
          <Text component={Link} to="/roasters" size="sm" c="blue">Roasters</Text>
          <Text size="sm">{roasterName}</Text>
          <Text size="sm">Products</Text>
        </Breadcrumbs>
        <Group justify="space-between" align="center">
          <Title order={2}>{roasterName} — Products</Title>
          <Group gap="md">
            <Text c="dimmed" size="sm">{data?.totalElements ?? 0} total</Text>
            <Switch
              label="Active only"
              checked={activeOnly}
              onChange={(e) => { setActiveOnly(e.currentTarget.checked); setPage(1); }}
              size="sm"
            />
          </Group>
        </Group>
      </Stack>

      <Table striped highlightOnHover withTableBorder>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Name</Table.Th>
            <Table.Th>Type</Table.Th>
            <Table.Th>Roast</Table.Th>
            <Table.Th>Origin</Table.Th>
            <Table.Th>Latest price</Table.Th>
            <Table.Th>Variants</Table.Th>
            <Table.Th>Tags</Table.Th>
            <Table.Th>Status</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {isLoading && (
            <Table.Tr>
              <Table.Td colSpan={8}><Text c="dimmed" size="sm">Loading…</Text></Table.Td>
            </Table.Tr>
          )}
          {data?.content.map((p) => (
            <Table.Tr key={p.id} opacity={p.active ? 1 : 0.55}>
              <Table.Td>
                {p.productUrl
                  ? (
                    <Anchor href={p.productUrl} target="_blank" rel="noreferrer" size="sm" fw={500}>
                      {p.name}
                    </Anchor>
                  )
                  : <Text size="sm" fw={500}>{p.name}</Text>
                }
              </Table.Td>
              <Table.Td>
                {p.productType
                  ? <Badge variant="outline" size="sm">{p.productType}</Badge>
                  : <Text size="sm" c="dimmed">—</Text>
                }
              </Table.Td>
              <Table.Td>
                <Text size="sm">{p.roastLevel ?? '—'}</Text>
              </Table.Td>
              <Table.Td>
                <Text size="sm">{originLabel(p)}</Text>
              </Table.Td>
              <Table.Td>
                <Text size="sm">{latestPrice(p.variants)}</Text>
              </Table.Td>
              <Table.Td>
                <Text size="sm" c="dimmed">{p.variants.length}</Text>
              </Table.Td>
              <Table.Td>
                <Group gap={4}>
                  {p.decaf && <Badge size="xs" color="gray">Decaf</Badge>}
                  {p.offersGrinding && <Badge size="xs" color="grape">Grinding</Badge>}
                </Group>
              </Table.Td>
              <Table.Td>
                <Badge size="sm" color={p.active ? 'green' : 'gray'}>
                  {p.active ? 'Active' : 'Inactive'}
                </Badge>
              </Table.Td>
            </Table.Tr>
          ))}
          {!isLoading && data?.content.length === 0 && (
            <Table.Tr>
              <Table.Td colSpan={8}><Text c="dimmed" size="sm">No products found.</Text></Table.Td>
            </Table.Tr>
          )}
        </Table.Tbody>
      </Table>

      {(data?.totalPages ?? 0) > 1 && (
        <Group justify="center">
          <Pagination total={data?.totalPages ?? 1} value={page} onChange={setPage} />
        </Group>
      )}
    </Stack>
  );
}
