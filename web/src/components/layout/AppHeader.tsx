import { AppShell, Burger, Button, Group, Text } from '@mantine/core';
import { Link } from 'react-router-dom';
import { config } from '../../config';
import { useAuth } from '../../hooks/useAuth';

interface Props {
  navOpened: boolean;
  onNavToggle: () => void;
}

export function AppHeader({ navOpened, onNavToggle }: Props) {
  const { isAuthenticated, login, logout } = useAuth();

  return (
    <AppShell.Header>
      <Group h="100%" px="md" justify="space-between">
        <Group gap="sm">
          <Burger opened={navOpened} onClick={onNavToggle} hiddenFrom="sm" size="sm" />
          <Text
            component={Link}
            to="/"
            fw={700}
            size="xl"
            ff="heading"
            style={{ textDecoration: 'none', color: 'inherit' }}
          >
            {config.appName}
          </Text>
        </Group>
        {isAuthenticated ? (
          <Button variant="subtle" size="sm" onClick={logout}>Sign out</Button>
        ) : (
          <Button variant="subtle" size="sm" onClick={login}>Sign in</Button>
        )}
      </Group>
    </AppShell.Header>
  );
}
