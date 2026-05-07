import { useState } from 'react';
import {
  Badge,
  Button,
  Group,
  Stack,
  Table,
  Text,
  Title,
  Alert,
} from '@mantine/core';
import { IconAlertCircle, IconRefresh, IconEyeOff } from '@tabler/icons-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../auth/AuthProvider';
import { listRoasters, triggerRoaster, deactivateProducts, type AdminRoaster } from '../services/adminApi';

function statusColor(status: AdminRoaster['lastRunStatus']) {
  if (!status) return 'gray';
  if (status === 'COMPLETED') return 'green';
  if (status === 'IN_PROGRESS') return 'blue';
  return 'red';
}

function moderationColor(status: AdminRoaster['moderationStatus']) {
  if (status === 'APPROVED') return 'green';
  if (status === 'REJECTED') return 'red';
  return 'yellow';
}

function formatDate(iso: string | null) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString();
}

export function RoastersPage() {
  const { getToken, isAdmin } = useAuth();
  const qc = useQueryClient();
  const [actionError, setActionError] = useState<string | null>(null);

  const { data: roasters, isLoading, error } = useQuery({
    queryKey: ['admin-roasters'],
    queryFn: () => listRoasters(getToken),
    enabled: isAdmin,
  });

  const triggerMutation = useMutation({
    mutationFn: (id: string) => triggerRoaster(getToken, id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-roasters'] }),
    onError: (e: Error) => setActionError(e.message),
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => deactivateProducts(getToken, id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-roasters'] }),
    onError: (e: Error) => setActionError(e.message),
  });

  if (!isAdmin) {
    return (
      <Alert icon={<IconAlertCircle />} color="red" title="Access denied">
        You need the admin role to view this page.
      </Alert>
    );
  }

  if (isLoading) return <Text>Loading...</Text>;
  if (error) return <Alert color="red">{String(error)}</Alert>;

  return (
    <Stack gap="md">
      <Group justify="space-between">
        <Title order={2}>Roasters</Title>
        <Button
          variant="subtle"
          leftSection={<IconRefresh size={16} />}
          onClick={() => qc.invalidateQueries({ queryKey: ['admin-roasters'] })}
        >
          Refresh
        </Button>
      </Group>

      {actionError && (
        <Alert icon={<IconAlertCircle />} color="red" withCloseButton onClose={() => setActionError(null)}>
          {actionError}
        </Alert>
      )}

      <Table striped highlightOnHover withTableBorder>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Name</Table.Th>
            <Table.Th>Integration</Table.Th>
            <Table.Th>Moderation</Table.Th>
            <Table.Th>Last Run</Table.Th>
            <Table.Th>Status</Table.Th>
            <Table.Th>Actions</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {roasters?.map((r) => (
            <Table.Tr key={r.id} opacity={r.active ? 1 : 0.5}>
              <Table.Td>
                <Stack gap={2}>
                  <Text fw={500}>{r.name}</Text>
                  {r.pendingRefresh && <Badge size="xs" color="orange">queued</Badge>}
                </Stack>
              </Table.Td>
              <Table.Td>
                <Badge variant="outline" size="sm">{r.integrationType}</Badge>
              </Table.Td>
              <Table.Td>
                <Badge color={moderationColor(r.moderationStatus)} size="sm">
                  {r.moderationStatus}
                </Badge>
              </Table.Td>
              <Table.Td>
                <Text size="sm" c="dimmed">{formatDate(r.lastRunCompletedAt ?? r.lastRunStartedAt)}</Text>
              </Table.Td>
              <Table.Td>
                {r.lastRunStatus
                  ? <Badge color={statusColor(r.lastRunStatus)} size="sm">{r.lastRunStatus}</Badge>
                  : <Text size="sm" c="dimmed">never</Text>
                }
              </Table.Td>
              <Table.Td>
                <Group gap="xs">
                  <Button
                    size="xs"
                    variant="light"
                    leftSection={<IconRefresh size={14} />}
                    loading={triggerMutation.isPending && triggerMutation.variables === r.id}
                    disabled={r.pendingRefresh || r.lastRunStatus === 'IN_PROGRESS'}
                    onClick={() => triggerMutation.mutate(r.id)}
                  >
                    Re-run
                  </Button>
                  <Button
                    size="xs"
                    variant="light"
                    color="red"
                    leftSection={<IconEyeOff size={14} />}
                    loading={deactivateMutation.isPending && deactivateMutation.variables === r.id}
                    onClick={() => deactivateMutation.mutate(r.id)}
                  >
                    Hide Products
                  </Button>
                </Group>
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Stack>
  );
}