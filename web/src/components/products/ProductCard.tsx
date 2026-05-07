import { Badge, Card, Group, Stack, Text } from '@mantine/core';
import { Link } from 'react-router-dom';
import type { Product } from '../../services/productsApi';

const ROAST_COLORS: Record<string, string> = {
  LIGHT: 'yellow', MEDIUM: 'orange', MEDIUM_DARK: 'red', DARK: 'dark',
};
const PROCESS_COLORS: Record<string, string> = {
  WASHED: 'blue', NATURAL: 'red', HONEY: 'yellow', ANAEROBIC: 'grape',
};

const fmtLabel = (s: string) =>
  s.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());

interface Props {
  product: Product;
}

export function ProductCard({ product }: Props) {
  const origin = [product.originRegion, product.originCountry]
    .filter(Boolean)
    .join(', ');

  const isLimitedAvailability =
    product.availabilityType && product.availabilityType !== 'STANDARD';

  const variants = product.variants ?? [];
  const pricesPerOz = variants.map(v => v.pricePerOz).filter((p): p is number => p != null);
  const minPricePerOz = pricesPerOz.length > 0 ? Math.min(...pricesPerOz) : null;
  const isOutOfStock = variants.length > 0 && variants.every(v => v.inStock === false);

  return (
    <Card
      component={Link}
      to={`/products/${product.id}`}
      shadow="xs"
      radius="md"
      withBorder
      style={{ textDecoration: 'none', color: 'inherit', cursor: 'pointer' }}
    >
      <Stack gap="xs">
        <Group justify="space-between" align="flex-start" wrap="nowrap">
          <Text size="xs" c="dimmed" fw={500} tt="uppercase" style={{ letterSpacing: '0.05em' }}>
            {product.roaster.name}
          </Text>
          <Group gap={4} wrap="nowrap" style={{ flexShrink: 0 }}>
            {product.decaf && (
              <Badge size="xs" variant="outline" color="gray">Decaf</Badge>
            )}
            {isLimitedAvailability && (
              <Badge size="xs" color="orange">
                {fmtLabel(product.availabilityType!)}
              </Badge>
            )}
            {isOutOfStock && (
              <Badge size="xs" color="red" variant="light">Out of stock</Badge>
            )}
          </Group>
        </Group>

        <Group justify="space-between" align="baseline" wrap="nowrap">
          <Text fw={600} ff="heading" size="lg" lh={1.3}>
            {product.name}
          </Text>
          {minPricePerOz != null && (
            <Text fw={600} size="sm" style={{ flexShrink: 0 }}>
              {pricesPerOz.length > 1 ? 'from ' : ''}${minPricePerOz.toFixed(2)}/oz
            </Text>
          )}
        </Group>

        {(product.roastLevel || product.process || origin) && (
          <Group gap="xs" wrap="wrap">
            {product.roastLevel && (
              <Badge
                size="sm"
                color={ROAST_COLORS[product.roastLevel] ?? 'gray'}
                variant="light"
              >
                {fmtLabel(product.roastLevel)}
              </Badge>
            )}
            {product.process && (
              <Badge
                size="sm"
                color={PROCESS_COLORS[product.process] ?? 'gray'}
                variant="light"
              >
                {fmtLabel(product.process)}
              </Badge>
            )}
            {origin && (
              <Badge size="sm" color="teal" variant="light">
                {origin}
              </Badge>
            )}
          </Group>
        )}

        {product.flavorProfile && product.flavorProfile.length > 0 && (
          <Text size="sm" c="dimmed">
            {product.flavorProfile.map(fmtLabel).join(' · ')}
          </Text>
        )}

        {product.brewMethods && product.brewMethods.length > 0 && (
          <Text size="xs" c="dimmed">
            {product.brewMethods.map(fmtLabel).join(' · ')}
          </Text>
        )}
      </Stack>
    </Card>
  );
}
