import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import keycloak from './keycloak';
import { setTokenGetter } from '../services/client';

interface AuthContextValue {
  authenticated: boolean;
  loading: boolean;
  login: () => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue>({
  authenticated: false,
  loading: true,
  login: () => {},
  logout: () => {},
});

export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authenticated, setAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);
  const initialized = useRef(false);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    keycloak
      .init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      })
      .then((auth) => {
        setAuthenticated(auth);
        if (auth) {
          setTokenGetter(async () => {
            await keycloak.updateToken(30);
            return keycloak.token!;
          });
        }
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  return (
    <AuthContext.Provider value={{
      authenticated,
      loading,
      login: () => keycloak.login(),
      logout: () => keycloak.logout({ redirectUri: window.location.origin }),
    }}>
      {children}
    </AuthContext.Provider>
  );
}
