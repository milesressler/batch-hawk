import { Anchor, Badge, Divider, Group, Skeleton, Stack, Text, Title, Tooltip } from '@mantine/core';
import { IconClock, IconScissors } from '@tabler/icons-react';
import type { ProductObservation } from '../services/productsApi';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import productsApi from '../services/productsApi';

const fmtLabel = (s: string) =>
  s.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());

function VariantsTable({ variants }: { variants: ProductObservation[] }) {
  if (variants.length === 0) return null;

  const observedAt = variants[0].observedAt;

  return (
    <Stack gap="xs">
      <Group justify="space-between" align="center">
        <Text size="xs" c="dimmed" tt="uppercase" fw={500}>Pricing</Text>
        <Tooltip label={`Last checked ${new Date(observedAt).toLocaleString()}`} withArrow>
          <IconClock size={14} color="var(--mantine-color-dimmed)" style={{ cursor: 'default' }} />
        </Tooltip>
      </Group>
      <Divider />
      {variants.map((v) => {
        const bagLabel = v.bagSize != null ? `${v.bagSize}${v.bagSizeUnit ?? ''}` : '—';
        return (
          <Group key={v.id} justify="space-between" align="center">
            <Text size="sm" fw={500}>{bagLabel}</Text>
            <Group gap="lg" align="center">
              {v.pricePerOz != null && (
                <Text size="xs" c="dimmed">${v.pricePerOz.toFixed(2)}/oz</Text>
              )}
              {v.priceUsd != null && (
                <Text fw={600} size="sm" w={60} ta="right">${v.priceUsd.toFixed(2)}</Text>
              )}
              {v.inStock != null && (
                <Badge size="xs" color={v.inStock ? 'green' : 'red'} variant="dot" w={80} ta="center">
                  {v.inStock ? 'In stock' : 'Out of stock'}
                </Badge>
              )}
            </Group>
          </Group>
        );
      })}
    </Stack>
  );
}

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();

  const { data: product, isLoading } = useQuery({
    queryKey: ['product', id],
    queryFn: () => productsApi.getById(id!),
    enabled: !!id,
  });

  if (isLoading) {
    return (
      <Stack p="xl" gap="md" maw={700} mx="auto">
        <Skeleton height={32} width="60%" />
        <Skeleton height={20} width="40%" />
        <Skeleton height={80} />
      </Stack>
    );
  }

  if (!product) return null;

  const origin = [product.originRegion, product.originCountry].filter(Boolean).join(', ');

  return (
    <Stack p="xl" gap="md" maw={700} mx="auto">
      <Anchor component={Link} to="/" size="sm">← Back to browse</Anchor>

      <Text size="sm" c="dimmed" tt="uppercase" fw={500}>
        {product.roaster.name}
      </Text>
      <Title order={1} ff="heading">{product.name}</Title>

      <Group gap="xs">
        {product.roastLevel && <Badge>{fmtLabel(product.roastLevel)}</Badge>}
        {product.process && <Badge variant="light">{fmtLabel(product.process)}</Badge>}
        {origin && <Badge color="teal" variant="light">{origin}</Badge>}
        {product.decaf && <Badge color="gray" variant="outline">Decaf</Badge>}
        {product.offersGrinding && (
          <Badge color="violet" variant="light" leftSection={<IconScissors size={10} />}>
            Grinding available
          </Badge>
        )}
      </Group>

      {product.variants && product.variants.length > 0 && (
        <VariantsTable variants={product.variants} />
      )}

      {product.flavorProfile && product.flavorProfile.length > 0 && (
        <Text c="dimmed">{product.flavorProfile.map(fmtLabel).join(' · ')}</Text>
      )}

      {product.description && <Text>{product.description}</Text>}

      {product.productUrl ? (
        <Anchor href={product.productUrl} target="_blank" rel="noopener noreferrer">
          Buy from {product.roaster.name} →
        </Anchor>
      ) : product.roaster.websiteUrl && (
        <Anchor href={product.roaster.websiteUrl} target="_blank" rel="noopener noreferrer">
          Visit {product.roaster.name} →
        </Anchor>
      )}
    </Stack>
  );
}
