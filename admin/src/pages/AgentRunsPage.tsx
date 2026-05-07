import { useState } from 'react';
import { Alert, Badge, Group, Pagination, Stack, Table, Text, Title, Tooltip } from '@mantine/core';
import { IconAlertCircle } from '@tabler/icons-react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider';
import { listAgentRuns, type AgentRun } from '../services/adminApi';

function statusColor(status: AgentRun['status']) {
  if (status === 'COMPLETED') return 'green';
  if (status === 'IN_PROGRESS') return 'blue';
  if (status === 'FAILED') return 'red';
  return 'gray';
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

export function AgentRunsPage() {
  const { getToken } = useAuth();
  const [page, setPage] = useState(1);

  const { data, isLoading, error } = useQuery({
    queryKey: ['admin-runs', page],
    queryFn: () => listAgentRuns(getToken, page - 1, 25),
  });

  if (isLoading) return <Text>Loading...</Text>;
  if (error) return <Alert icon={<IconAlertCircle />} color="red">{String(error)}</Alert>;

  return (
    <Stack gap="md">
      <Group justify="space-between">
        <Title order={2}>Agent Runs</Title>
        <Text c="dimmed" size="sm">{data?.totalElements ?? 0} total</Text>
      </Group>

      <Table striped highlightOnHover withTableBorder>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Roaster</Table.Th>
            <Table.Th>Status</Table.Th>
            <Table.Th>Started</Table.Th>
            <Table.Th>Duration</Table.Th>
            <Table.Th>Tokens</Table.Th>
            <Table.Th>Notes</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {data?.content.map((run) => (
            <Table.Tr key={run.id}>
              <Table.Td>
                <Text
                  component={Link}
                  to="/"
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
              <Table.Td>
                {run.feedbackNotes
                  ? (
                    <Tooltip label={run.feedbackNotes} multiline maw={300}>
                      <Text size="sm" truncate maw={200} style={{ cursor: 'default' }}>
                        {run.feedbackNotes}
                      </Text>
                    </Tooltip>
                  )
                  : <Text size="sm" c="dimmed">—</Text>
                }
              </Table.Td>
            </Table.Tr>
          ))}
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
