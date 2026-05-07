export const config = {
  appName: import.meta.env.VITE_APP_NAME ?? 'Batch Hawk',
  appTagline: import.meta.env.VITE_APP_TAGLINE ?? 'Specialty coffee, price-compared.',
  keycloak: {
    url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180',
    realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'batchhawk',
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'batch-hawk-web',
  },
} as const;
