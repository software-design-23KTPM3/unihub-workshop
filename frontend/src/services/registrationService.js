import { API_ENDPOINTS } from './apiEndpoints.js';
import { httpClient } from './httpClient.js';

export function createIdempotencyKey() {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID();
  }

  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
    const random = Math.random() * 16 | 0;
    const value = char === 'x' ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}

export function registerWorkshop(workshopId, options = {}) {
  const idempotencyKey = options.idempotencyKey || createIdempotencyKey();

  return httpClient.post(
    API_ENDPOINTS.registrations.create,
    { workshopId, idempotencyKey },
  );
}

export function startRegistrationPayment(registrationId, options = {}) {
  const idempotencyKey = options.idempotencyKey || createIdempotencyKey();
  return httpClient.post(
    API_ENDPOINTS.registrations.startPayment(registrationId),
    { idempotencyKey },
  );
}

export function getMyRegistrations() {
  return httpClient.get(API_ENDPOINTS.registrations.mine);
}

export function getAllRegistrations(filters = {}) {
  return httpClient.get(API_ENDPOINTS.admin.registrations, { query: filters });
}

export function getRegistrationById(id) {
  return httpClient.get(API_ENDPOINTS.registrations.detail(id));
}

export function getRegistrationQrBlob(id, options) {
  return httpClient.blob(API_ENDPOINTS.registrations.qrImage(id), options);
}
