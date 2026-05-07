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
  ActionIcon,
  Tooltip,
} from '@mantine/core';
import {
  IconAlertCircle,
  IconRefresh,
  IconEyeOff,
  IconPlayerPlay,
  IconPencil,
  IconPlus,
  IconEye,
  IconPackages,
} from '@tabler/icons-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider';
import {
  listRoasters,
  triggerRoaster,
  deactivateProducts,
  createRoaster,
  updateRoaster,
  type AdminRoaster,
  type AdminRoasterRequest,
} from '../services/adminApi';
import { RoasterFormModal } from '../components/RoasterFormModal';

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
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AdminRoaster | null>(null);

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

  const createMutation = useMutation({
    mutationFn: (data: AdminRoasterRequest) => createRoaster(getToken, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['admin-roasters'] }); setModalOpen(false); },
    onError: (e: Error) => setActionError(e.message),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: AdminRoasterRequest }) => updateRoaster(getToken, id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-roasters'] });
      setModalOpen(false);
      setEditing(null);
    },
    onError: (e: Error) => setActionError(e.message),
  });

  function openAdd() { setEditing(null); setModalOpen(true); }
  function openEdit(r: AdminRoaster) { setEditing(r); setModalOpen(true); }
  function closeModal() { setModalOpen(false); setEditing(null); }

  function handleSubmit(data: AdminRoasterRequest) {
    if (editing) {
      updateMutation.mutate({ id: editing.id, data });
    } else {
      createMutation.mutate(data);
    }
  }

  function toggleActive(r: AdminRoaster) {
    updateMutation.mutate({ id: r.id, data: { active: !r.active } });
  }

  if (!isAdmin) {
    return (
      <Alert icon={<IconAlertCircle />} color="red" title="Access denied">
        You need the admin role to view this page.
      </Alert>
    );
  }

  if (isLoading) return <Text>Loading...</Text>;
  if (error) return <Alert color="red">{String(error)}</Alert>;

  const isMutating = createMutation.isPending || updateMutation.isPending;

  return (
    <>
      <Stack gap="md">
        <Group justify="space-between">
          <Title order={2}>Roasters</Title>
          <Group gap="xs">
            <Button
              variant="subtle"
              leftSection={<IconRefresh size={16} />}
              onClick={() => qc.invalidateQueries({ queryKey: ['admin-roasters'] })}
            >
              Refresh
            </Button>
            <Button leftSection={<IconPlus size={16} />} onClick={openAdd}>
              Add Roaster
            </Button>
          </Group>
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
                    <Tooltip label="View products">
                      <ActionIcon variant="subtle" color="teal" component={Link} to={`/roasters/${r.id}/products`}>
                        <IconPackages size={16} />
                      </ActionIcon>
                    </Tooltip>
                    <Tooltip label="Edit">
                      <ActionIcon variant="subtle" onClick={() => openEdit(r)}>
                        <IconPencil size={16} />
                      </ActionIcon>
                    </Tooltip>
                    <Tooltip label={r.active ? 'Deactivate roaster' : 'Activate roaster'}>
                      <ActionIcon
                        variant="subtle"
                        color={r.active ? 'gray' : 'green'}
                        loading={updateMutation.isPending && updateMutation.variables?.id === r.id && updateMutation.variables?.data.active !== undefined}
                        onClick={() => toggleActive(r)}
                      >
                        {r.active ? <IconEyeOff size={16} /> : <IconEye size={16} />}
                      </ActionIcon>
                    </Tooltip>
                    <Tooltip label="Trigger re-run">
                      <ActionIcon
                        variant="subtle"
                        color="blue"
                        loading={triggerMutation.isPending && triggerMutation.variables === r.id}
                        disabled={r.pendingRefresh || r.lastRunStatus === 'IN_PROGRESS'}
                        onClick={() => triggerMutation.mutate(r.id)}
                      >
                        <IconPlayerPlay size={16} />
                      </ActionIcon>
                    </Tooltip>
                    <Tooltip label="Hide all products">
                      <ActionIcon
                        variant="subtle"
                        color="red"
                        loading={deactivateMutation.isPending && deactivateMutation.variables === r.id}
                        onClick={() => deactivateMutation.mutate(r.id)}
                      >
                        <IconEyeOff size={16} />
                      </ActionIcon>
                    </Tooltip>
                  </Group>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Stack>

      <RoasterFormModal
        opened={modalOpen}
        onClose={closeModal}
        onSubmit={handleSubmit}
        initial={editing}
        loading={isMutating}
      />
    </>
  );
}
