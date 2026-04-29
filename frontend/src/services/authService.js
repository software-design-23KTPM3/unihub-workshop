import { USE_MOCK } from '../config/api.js';
import { mockUsers } from '../mocks/users.mock.js';
import { storage } from '../utils/storage.js';
import { API_ENDPOINTS } from './apiEndpoints.js';
import { httpClient } from './httpClient.js';

const AUTH_USER_KEY = 'unihub.auth.user';
const SUPPORTED_ROLES = ['STUDENT', 'ORGANIZER'];

function isSupportedRole(role) {
  return SUPPORTED_ROLES.includes(role);
}

function sanitizeUser(user) {
  if (!user) {
    return null;
  }

  const { password, ...safeUser } = user;
  return safeUser;
}

function persistUser(user) {
  if (!isSupportedRole(user?.role)) {
    throw new Error('Tài khoản không có quyền truy cập web UniHub Workshop.');
  }

  storage.set(AUTH_USER_KEY, user);
  return user;
}

export async function login(email, password) {
  if (!USE_MOCK) {
    const response = await httpClient.post(API_ENDPOINTS.auth.login, { email, password });
    const user = response.user || response;
    const token = response.token || response.accessToken;
    return persistUser(token ? { ...user, token } : user);
  }

  const normalizedEmail = email?.trim().toLowerCase();
  const user = mockUsers.find(
    (item) => item.email === normalizedEmail && item.password === password,
  );

  if (!user) {
    throw new Error('Email hoặc mật khẩu không đúng.');
  }

  const safeUser = sanitizeUser(user);
  return persistUser(safeUser);
}

export function getDemoAccounts() {
  if (!USE_MOCK) {
    return [];
  }

  return mockUsers.map(sanitizeUser);
}

export function logout() {
  storage.remove(AUTH_USER_KEY);
}

export function getCurrentUser() {
  const user = storage.get(AUTH_USER_KEY);

  if (user && !isSupportedRole(user.role)) {
    storage.remove(AUTH_USER_KEY);
    return null;
  }

  return user;
}

export function isAuthenticated() {
  return Boolean(getCurrentUser());
}

export function hasRole(allowedRoles) {
  const user = getCurrentUser();
  const roles = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];
  return Boolean(user && roles.includes(user.role));
}
