import { API_ENDPOINTS } from './apiEndpoints.js';
import { httpClient } from './httpClient.js';
import { getCurrentUser } from './authService.js';

export function registerWorkshop(workshopId) {
  const user = getCurrentUser();
  const studentId = user?.studentId || user?.id;

  return httpClient.post(
    API_ENDPOINTS.registrations.create,
    { workshopId, studentId }
  );
}

export function completeMockPayment(registrationId) {
  return httpClient.post(API_ENDPOINTS.registrations.mockPaymentSuccess(registrationId));
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
