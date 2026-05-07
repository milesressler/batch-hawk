import { createContext, useContext, useEffect, useState } from 'react';
import { keycloak } from './keycloak';
import { setTokenGetter } from '../services/client';

interface AuthContextValue {
  isAuthenticated: boolean;
  isAdmin: boolean;
  user: { name: string; email: string } | null;
  login: () => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue>({
  isAuthenticated: false,
  isAdmin: false,
  user: null,
  login: () => {},
  logout: () => {},
});

function parseUser() {
  const p = keycloak.tokenParsed;
  if (!p) return null;
  return {
    name: (p['name'] ?? p['preferred_username'] ?? '') as string,
    email: (p['email'] ?? '') as string,
  };
}

function wireTokenGetter() {
  setTokenGetter(async () => {
    try {
      await keycloak.updateToken(30);
    } catch {
      keycloak.login();
    }
    return keycloak.token ?? '';
  });
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [initialized, setInitialized] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState<AuthContextValue['user']>(null);

  useEffect(() => {
    keycloak
      .init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        pkceMethod: 'S256',
      })
      .then((authenticated) => {
        setIsAuthenticated(authenticated);
        if (authenticated) {
          setUser(parseUser());
          wireTokenGetter();
        }
      })
      .catch(() => {
        // Keycloak unavailable — app still works for anonymous browsing
      })
      .finally(() => setInitialized(true));

    keycloak.onAuthSuccess = () => {
      setIsAuthenticated(true);
      setUser(parseUser());
      wireTokenGetter();
    };

    keycloak.onAuthLogout = () => {
      setIsAuthenticated(false);
      setUser(null);
      setTokenGetter(null);
    };

    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).catch(() => {
        setIsAuthenticated(false);
        setUser(null);
        setTokenGetter(null);
      });
    };
  }, []);

  if (!initialized) return null;

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated,
        user,
        login: () => keycloak.login(),
        logout: () => keycloak.logout({ redirectUri: window.location.origin }),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
