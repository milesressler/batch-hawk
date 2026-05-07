import { AppShell, Button, Group, NavLink, Text } from '@mantine/core';
import { IconList, IconRefresh } from '@tabler/icons-react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../auth/AuthProvider';

export function AdminShell({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, login, logout } = useAuth();
  const { pathname } = useLocation();

  return (
    <AppShell header={{ height: 56 }} navbar={{ width: 180, breakpoint: 'sm' }} padding="md">
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
      <AppShell.Navbar p="xs">
        <NavLink
          component={Link}
          to="/"
          label="Roasters"
          leftSection={<IconList size={16} />}
          active={pathname === '/'}
        />
        <NavLink
          component={Link}
          to="/runs"
          label="Agent Runs"
          leftSection={<IconRefresh size={16} />}
          active={pathname === '/runs'}
        />
      </AppShell.Navbar>
      <AppShell.Main>
        {children}
      </AppShell.Main>
    </AppShell>
  );
}
