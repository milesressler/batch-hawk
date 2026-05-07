export const config = {
  appName: 'Batch Hawk Admin',
  keycloak: {
    url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180',
    realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'batchhawk',
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'batch-hawk-admin',
  },
} as const;