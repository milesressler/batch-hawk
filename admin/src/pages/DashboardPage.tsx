import { Alert, Badge, Group, Paper, SimpleGrid, Stack, Table, Text, Title } from '@mantine/core';
import { IconAlertCircle } from '@tabler/icons-react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider';
import { listAgentRuns, listRoasters, type AgentRun } from '../services/adminApi';

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

interface StatCardProps {
  label: string;
  value: number | string;
  color?: string;
}

function StatCard({ label, value, color }: StatCardProps) {
  return (
    <Paper withBorder p="md" radius="md">
      <Text size="xs" tt="uppercase" fw={700} c="dimmed">{label}</Text>
      <Text size="xl" fw={700} c={color}>{value}</Text>
    </Paper>
  );
}

export function DashboardPage() {
  const { getToken } = useAuth();

  const { data: roasters, isLoading: roastersLoading, error: roastersError } = useQuery({
    queryKey: ['admin-roasters'],
    queryFn: () => listRoasters(getToken),
  });

  const { data: runs, isLoading: runsLoading, error: runsError } = useQuery({
    queryKey: ['admin-runs', 0],
    queryFn: () => listAgentRuns(getToken, 0, 25),
  });

  const error = roastersError || runsError;
  if (error) return <Alert icon={<IconAlertCircle />} color="red">{String(error)}</Alert>;

  const totalRoasters = roasters?.length ?? 0;
  const activeRoasters = roasters?.filter((r) => r.active).length ?? 0;
  const pendingApproval = roasters?.filter((r) => r.moderationStatus === 'PENDING').length ?? 0;
  const queued = roasters?.filter((r) => r.pendingRefresh).length ?? 0;

  const recentRuns = runs?.content ?? [];
  const completed = recentRuns.filter((r) => r.status === 'COMPLETED').length;
  const failed = recentRuns.filter((r) => r.status === 'FAILED').length;
  const inProgress = recentRuns.filter((r) => r.status === 'IN_PROGRESS').length;
  const totalTokens = recentRuns.reduce((sum, r) => sum + (r.inputTokens ?? 0) + (r.outputTokens ?? 0), 0);

  return (
    <Stack gap="lg">
      <Title order={2}>Dashboard</Title>

      <Stack gap="xs">
        <Text size="sm" fw={600} c="dimmed" tt="uppercase">Roasters</Text>
        <SimpleGrid cols={{ base: 2, sm: 4 }}>
          <StatCard label="Total" value={roastersLoading ? '…' : totalRoasters} />
          <StatCard label="Active" value={roastersLoading ? '…' : activeRoasters} color="green" />
          <StatCard label="Pending approval" value={roastersLoading ? '…' : pendingApproval} color={pendingApproval > 0 ? 'yellow' : undefined} />
          <StatCard label="Queued for refresh" value={roastersLoading ? '…' : queued} color={queued > 0 ? 'orange' : undefined} />
        </SimpleGrid>
      </Stack>

      <Stack gap="xs">
        <Text size="sm" fw={600} c="dimmed" tt="uppercase">Agent Runs (last 25)</Text>
        <SimpleGrid cols={{ base: 2, sm: 4 }}>
          <StatCard label="Completed" value={runsLoading ? '…' : completed} color="green" />
          <StatCard label="Failed" value={runsLoading ? '…' : failed} color={failed > 0 ? 'red' : undefined} />
          <StatCard label="In progress" value={runsLoading ? '…' : inProgress} color={inProgress > 0 ? 'blue' : undefined} />
          <StatCard label="Tokens used" value={runsLoading ? '…' : `${(totalTokens / 1000).toFixed(1)}k`} />
        </SimpleGrid>
      </Stack>

      <Stack gap="xs">
        <Group justify="space-between">
          <Text size="sm" fw={600} c="dimmed" tt="uppercase">Recent Runs</Text>
          <Text component={Link} to="/runs" size="sm" c="blue">View all →</Text>
        </Group>
        <Paper withBorder>
          <Table striped highlightOnHover>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Roaster</Table.Th>
                <Table.Th>Status</Table.Th>
                <Table.Th>Started</Table.Th>
                <Table.Th>Duration</Table.Th>
                <Table.Th>Tokens</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {runsLoading && (
                <Table.Tr>
                  <Table.Td colSpan={5}><Text c="dimmed" size="sm">Loading…</Text></Table.Td>
                </Table.Tr>
              )}
              {recentRuns.slice(0, 8).map((run) => (
                <Table.Tr key={run.id}>
                  <Table.Td>
                    <Text
                      component={Link}
                      to={`/runs/${run.id}`}
                      size="sm"
                      fw={500}
                      style={{ textDecoration: 'none', color: 'inherit' }}
                    >
                      {run.roasterName}
                    </Text>
                  </Table.Td>
                  <Table.Td>
                    <Badge color={statusColor(run.status)} size="sm">{run.status}</Badge>
                  </Table.Td>
                  <Table.Td>
                    <Text size="sm" c="dimmed">{formatDate(run.startedAt)}</Text>
                  </Table.Td>
                  <Table.Td>
                    <Text size="sm">{duration(run.startedAt, run.completedAt)}</Text>
                  </Table.Td>
                  <Table.Td>
                    {run.inputTokens != null
                      ? <Text size="sm">{((run.inputTokens + (run.outputTokens ?? 0)) / 1000).toFixed(1)}k</Text>
                      : <Text size="sm" c="dimmed">—</Text>
                    }
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Paper>
      </Stack>
    </Stack>
  );
}
