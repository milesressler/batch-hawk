import { Anchor, Badge, Group, Skeleton, Stack, Text, Title } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import productsApi from '../services/productsApi';

const fmtLabel = (s: string) =>
  s.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());

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
      </Group>

      {product.flavorProfile && product.flavorProfile.length > 0 && (
        <Text c="dimmed">{product.flavorProfile.map(fmtLabel).join(' · ')}</Text>
      )}

      {product.description && <Text>{product.description}</Text>}

      {product.roaster.websiteUrl && (
        <Anchor href={product.roaster.websiteUrl} target="_blank" rel="noopener noreferrer">
          Visit {product.roaster.name} →
        </Anchor>
      )}
    </Stack>
  );
}
