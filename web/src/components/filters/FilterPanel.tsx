import { Button, Chip, Divider, Group, Stack, Switch, Text, TextInput, Title } from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';

export interface FilterState {
  roastLevel: string[];
  process: string[];
  productType: string[];
  availability: string[];
  decafOnly: boolean;
  keyword: string;
  typicalSizesOnly: boolean;
}

export const EMPTY_FILTERS: FilterState = {
  roastLevel: [],
  process: [],
  productType: [],
  availability: [],
  decafOnly: false,
  keyword: '',
  typicalSizesOnly: true,
};

const ROAST_LEVELS = ['LIGHT', 'MEDIUM', 'MEDIUM_DARK', 'DARK'];
const PROCESSES = ['WASHED', 'NATURAL', 'HONEY', 'ANAEROBIC', 'OTHER'];
const PRODUCT_TYPES = ['SINGLE_ORIGIN', 'BLEND'];
const AVAILABILITY = ['STANDARD', 'LIMITED', 'SEASONAL'];

const label = (s: string) =>
  s.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());

interface Props {
  filters: FilterState;
  onChange: (filters: FilterState) => void;
}

function hasActiveFilters(f: FilterState) {
  return (
    f.roastLevel.length > 0 ||
    f.process.length > 0 ||
    f.productType.length > 0 ||
    f.availability.length > 0 ||
    f.decafOnly ||
    f.keyword.trim().length > 0 ||
    !f.typicalSizesOnly
  );
}

function ChipSection({
  title,
  values,
  selected,
  onToggle,
}: {
  title: string;
  values: string[];
  selected: string[];
  onToggle: (v: string[]) => void;
}) {
  return (
    <Stack gap="xs">
      <Text size="xs" fw={600} tt="uppercase" c="dimmed">{title}</Text>
      <Chip.Group multiple value={selected} onChange={onToggle}>
        <Group gap="xs">
          {values.map((v) => (
            <Chip key={v} value={v} size="xs" variant="outline">
              {label(v)}
            </Chip>
          ))}
        </Group>
      </Chip.Group>
    </Stack>
  );
}

export function FilterPanel({ filters, onChange }: Props) {
  const set = <K extends keyof FilterState>(key: K, value: FilterState[K]) =>
    onChange({ ...filters, [key]: value });

  return (
    <Stack gap="lg" p="md">
      <Group justify="space-between" align="center">
        <Title order={6} tt="uppercase" c="dimmed">Filters</Title>
        {hasActiveFilters(filters) && (
          <Button variant="subtle" size="compact-xs" onClick={() => onChange(EMPTY_FILTERS)}>
            Clear all
          </Button>
        )}
      </Group>

      <TextInput
        placeholder="Search by name, roaster, flavor…"
        leftSection={<IconSearch size={14} />}
        value={filters.keyword}
        onChange={(e) => set('keyword', e.currentTarget.value)}
        size="sm"
      />
      <Divider />

      <ChipSection
        title="Roast Level"
        values={ROAST_LEVELS}
        selected={filters.roastLevel}
        onToggle={(v) => set('roastLevel', v)}
      />
      <Divider />
      <ChipSection
        title="Process"
        values={PROCESSES}
        selected={filters.process}
        onToggle={(v) => set('process', v)}
      />
      <Divider />
      <ChipSection
        title="Type"
        values={PRODUCT_TYPES}
        selected={filters.productType}
        onToggle={(v) => set('productType', v)}
      />
      <Divider />
      <ChipSection
        title="Availability"
        values={AVAILABILITY}
        selected={filters.availability}
        onToggle={(v) => set('availability', v)}
      />
      <Divider />
      <Switch
        label="Decaf only"
        checked={filters.decafOnly}
        onChange={(e) => set('decafOnly', e.currentTarget.checked)}
      />
      <Switch
        label="Typical sizes only (6–32 oz)"
        description="Hides bulk and sample-sized bags"
        checked={filters.typicalSizesOnly}
        onChange={(e) => set('typicalSizesOnly', e.currentTarget.checked)}
      />
    </Stack>
  );
}
