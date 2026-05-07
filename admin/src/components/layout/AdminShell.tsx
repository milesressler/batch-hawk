import { AppShell, Button, Group, Text } from '@mantine/core';
import { useAuth } from '../../auth/AuthProvider';

export function AdminShell({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, login, logout } = useAuth();

  return (
    <AppShell header={{ height: 56 }} padding="md">
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Text fw={700} size="xl" ff="heading">
            Batch Hawk Admin
          </Text>
          {isAuthenticated
            ? <Button variant="subtle" size="sm" onClick={logout}>Sign out</Button>
            : <Button variant="subtle" size="sm" onClick={login}>Sign in</Button>
          }
        </Group>
      </AppShell.Header>
      <AppShell.Main>
        {children}
      </AppShell.Main>
    </AppShell>
  );
}