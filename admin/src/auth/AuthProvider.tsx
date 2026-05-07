import { createContext, useContext, useEffect, useRef, useState } from 'react';
import { keycloak } from './keycloak';

interface AuthContextValue {
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: () => void;
  logout: () => void;
  getToken: () => Promise<string>;
}

const AuthContext = createContext<AuthContextValue>({
  isAuthenticated: false,
  isAdmin: false,
  login: () => {},
  logout: () => {},
  getToken: () => Promise.resolve(''),
});

function parseIsAdmin() {
  const parsed = keycloak.tokenParsed as { resource_access?: Record<string, { roles?: string[] }> } | undefined;
  const roles = parsed?.resource_access?.['batch-hawk-admin']?.roles ?? [];
  return roles.includes('admin');
}

async function refreshedToken(): Promise<string> {
  try {
    await keycloak.updateToken(30);
  } catch {
    keycloak.login();
  }
  return keycloak.token ?? '';
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [initialized, setInitialized] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isAdmin, setIsAdmin] = useState(false);
  const initStarted = useRef(false);

  useEffect(() => {
    if (initStarted.current) return;
    initStarted.current = true;

    keycloak
      .init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        pkceMethod: 'S256',
        checkLoginIframe: false,
      })
      .then((authenticated) => {
        setIsAuthenticated(authenticated);
        setIsAdmin(authenticated ? parseIsAdmin() : false);
      })
      .catch(() => {})
      .finally(() => setInitialized(true));

    keycloak.onAuthSuccess = () => {
      setIsAuthenticated(true);
      setIsAdmin(parseIsAdmin());
    };

    keycloak.onAuthLogout = () => {
      setIsAuthenticated(false);
      setIsAdmin(false);
    };

    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).catch(() => {
        setIsAuthenticated(false);
        setIsAdmin(false);
      });
    };
  }, []);

  if (!initialized) return null;

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated,
        isAdmin,
        login: () => keycloak.login(),
        logout: () => keycloak.logout({ redirectUri: window.location.origin }),
        getToken: refreshedToken,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);