import { Alert, Badge, Breadcrumbs, Group, Paper, SimpleGrid, Stack, Text, Title } from '@mantine/core';
import { IconAlertCircle } from '@tabler/icons-react';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider';
import { getAgentRun, type AgentRun } from '../services/adminApi';

function statusColor(status: AgentRun['status']) {
  if (status === 'COMPLETED') return 'green';
  if (status === 'IN_PROGRESS') return 'blue';
  return 'red';
}

function duration(startedAt: string | null, completedAt: string | null) {
  if (!startedAt || !completedAt) return '—';
  const secs = Math.round((new Date(completedAt).getTime() - new Date(startedAt).getTime()) / 1000);
  if (secs < 60) return `${secs}s`;
  return `${Math.floor(secs / 60)}m ${secs % 60}s`;
}

function formatDate(iso: string | null) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString();
}

function MetaItem({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <Stack gap={2}>
      <Text size="xs" tt="uppercase" fw={700} c="dimmed">{label}</Text>
      {children}
    </Stack>
  );
}

function NotesBlock({ label, text }: { label: string; text: string | null }) {
  if (!text) return null;
  return (
    <Paper withBorder p="md" radius="md">
      <Stack gap="xs">
        <Text size="xs" tt="uppercase" fw={700} c="dimmed">{label}</Text>
        <Text size="sm" style={{ whiteSpace: 'pre-wrap' }}>{text}</Text>
      </Stack>
    </Paper>
  );
}

export function RunDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { getToken } = useAuth();

  const { data: run, isLoading, error } = useQuery({
    queryKey: ['admin-run', id],
    queryFn: () => getAgentRun(getToken, Number(id)),
    enabled: !!id,
  });

  if (isLoading) return <Text>Loading…</Text>;
  if (error) return <Alert icon={<IconAlertCircle />} color="red">{String(error)}</Alert>;
  if (!run) return null;

  const totalTokens = (run.inputTokens ?? 0) + (run.outputTokens ?? 0);

  return (
    <Stack gap="lg">
      <Stack gap="xs">
        <Breadcrumbs>
          <Text component={Link} to="/runs" size="sm" c="blue">Agent Runs</Text>
          <Text size="sm">#{run.id}</Text>
        </Breadcrumbs>
        <Group align="center" gap="sm">
          <Title order={2}>{run.roasterName}</Title>
          <Badge color={statusColor(run.status)} size="lg">{run.status}</Badge>
        </Group>
      </Stack>

      <Paper withBorder p="md" radius="md">
        <SimpleGrid cols={{ base: 2, sm: 4 }}>
          <MetaItem label="Run ID">
            <Text size="sm" ff="monospace">#{run.id}</Text>
          </MetaItem>
          <MetaItem label="Started">
            <Text size="sm">{formatDate(run.startedAt)}</Text>
          </MetaItem>
          <MetaItem label="Completed">
            <Text size="sm">{formatDate(run.completedAt)}</Text>
          </MetaItem>
          <MetaItem label="Duration">
            <Text size="sm">{duration(run.startedAt, run.completedAt)}</Text>
          </MetaItem>
          <MetaItem label="Input tokens">
            <Text size="sm">{run.inputTokens != null ? run.inputTokens.toLocaleString() : '—'}</Text>
          </MetaItem>
          <MetaItem label="Output tokens">
            <Text size="sm">{run.outputTokens != null ? run.outputTokens.toLocaleString() : '—'}</Text>
          </MetaItem>
          <MetaItem label="Total tokens">
            <Text size="sm" fw={600}>{totalTokens > 0 ? `${(totalTokens / 1000).toFixed(1)}k` : '—'}</Text>
          </MetaItem>
        </SimpleGrid>
      </Paper>

      {run.fieldsFound && run.fieldsFound.length > 0 && (
        <Stack gap="xs">
          <Text size="xs" tt="uppercase" fw={700} c="dimmed">Fields found</Text>
          <Group gap="xs">
            {run.fieldsFound.map((field) => (
              <Badge key={field} variant="light" size="sm">
                {field.replace(/_/g, ' ').toLowerCase()}
              </Badge>
            ))}
          </Group>
        </Stack>
      )}

      <NotesBlock label="Feedback notes" text={run.feedbackNotes} />
      <NotesBlock label="Checkout notes" text={run.checkoutNotes} />
    </Stack>
  );
}
