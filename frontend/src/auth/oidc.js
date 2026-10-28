import Keycloak from 'keycloak-js';

let keycloak;

export function getKeycloak() {
  return keycloak;
}

function parseIssuer(issuerUri) {
  // http://localhost:8081/realms/office-link
  const match = String(issuerUri).match(/^(https?:\/\/[^/]+)\/realms\/([^/]+)/i);
  if (!match) {
    throw new Error('Invalid OIDC issuer URI');
  }
  return { url: match[1], realm: match[2] };
}

export async function initOidcClient(config) {
  if (!config?.enabled || !config.issuerUri || !config.clientId) {
    return null;
  }

  const { url, realm } = parseIssuer(config.issuerUri);
  keycloak = new Keycloak({
    url,
    realm,
    clientId: config.clientId,
  });

  const authenticated = await keycloak.init({
    onLoad: 'check-sso',
    pkceMethod: 'S256',
    checkLoginIframe: false,
  });

  return { keycloak, authenticated };
}

export async function loginWithOidc() {
  if (!keycloak) throw new Error('OIDC is not configured');
  await keycloak.login({ redirectUri: window.location.origin });
}

export async function logoutOidc() {
  if (keycloak?.authenticated) {
    await keycloak.logout({ redirectUri: window.location.origin });
  }
}

export function readOidcToken() {
  return keycloak?.token || null;
}
