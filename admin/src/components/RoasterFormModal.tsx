import { useEffect, useState } from 'react';
import { Button, Group, Modal, Select, Stack, Switch, TextInput } from '@mantine/core';
import type { AdminRoaster, AdminRoasterRequest, IntegrationType, ModerationStatus } from '../services/adminApi';

interface Props {
  opened: boolean;
  onClose: () => void;
  onSubmit: (data: AdminRoasterRequest) => void;
  initial?: AdminRoaster | null;
  loading: boolean;
}

const INTEGRATION_OPTIONS: { value: IntegrationType; label: string }[] = [
  { value: 'SHOPIFY', label: 'Shopify' },
  { value: 'WOO_COMMERCE', label: 'WooCommerce' },
  { value: 'SQUARE', label: 'Square' },
  { value: 'SQUARESPACE', label: 'Squarespace' },
  { value: 'CUSTOM', label: 'Custom' },
  { value: 'UNKNOWN', label: 'Unknown' },
];

const MODERATION_OPTIONS: { value: ModerationStatus; label: string }[] = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
];

export function RoasterFormModal({ opened, onClose, onSubmit, initial, loading }: Props) {
  const [name, setName] = useState('');
  const [websiteUrl, setWebsiteUrl] = useState('');
  const [emailListUrl, setEmailListUrl] = useState('');
  const [integrationType, setIntegrationType] = useState<IntegrationType>('UNKNOWN');
  const [moderationStatus, setModerationStatus] = useState<ModerationStatus>('PENDING');
  const [active, setActive] = useState(true);

  useEffect(() => {
    if (initial) {
      setName(initial.name);
      setWebsiteUrl(initial.websiteUrl ?? '');
      setEmailListUrl(initial.emailListUrl ?? '');
      setIntegrationType(initial.integrationType);
      setModerationStatus(initial.moderationStatus);
      setActive(initial.active);
    } else {
      setName('');
      setWebsiteUrl('');
      setEmailListUrl('');
      setIntegrationType('UNKNOWN');
      setModerationStatus('PENDING');
      setActive(true);
    }
  }, [initial, opened]);

  function handleSubmit() {
    onSubmit({
      name,
      websiteUrl: websiteUrl || undefined,
      emailListUrl: emailListUrl || undefined,
      integrationType,
      moderationStatus,
      active,
    });
  }

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={initial ? 'Edit Roaster' : 'Add Roaster'}
      size="md"
    >
      <Stack gap="sm">
        <TextInput
          label="Name"
          required
          value={name}
          onChange={(e) => setName(e.currentTarget.value)}
        />
        <TextInput
          label="Website URL"
          value={websiteUrl}
          onChange={(e) => setWebsiteUrl(e.currentTarget.value)}
        />
        <TextInput
          label="Email List URL"
          value={emailListUrl}
          onChange={(e) => setEmailListUrl(e.currentTarget.value)}
        />
        <Select
          label="Integration Type"
          data={INTEGRATION_OPTIONS}
          value={integrationType}
          onChange={(v) => setIntegrationType((v as IntegrationType) ?? 'UNKNOWN')}
        />
        <Select
          label="Moderation Status"
          data={MODERATION_OPTIONS}
          value={moderationStatus}
          onChange={(v) => setModerationStatus((v as ModerationStatus) ?? 'PENDING')}
        />
        <Switch
          label="Active"
          checked={active}
          onChange={(e) => setActive(e.currentTarget.checked)}
        />
        <Group justify="flex-end" mt="sm">
          <Button variant="subtle" onClick={onClose}>Cancel</Button>
          <Button onClick={handleSubmit} loading={loading} disabled={!name.trim()}>
            {initial ? 'Save' : 'Create'}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
