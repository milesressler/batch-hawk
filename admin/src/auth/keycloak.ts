import Keycloak from 'keycloak-js';
import { config } from '../config';

export const keycloak = new Keycloak({
  url: config.keycloak.url,
  realm: config.keycloak.realm,
  clientId: config.keycloak.clientId,
});