import { API_ENDPOINTS } from './apiEndpoints.js';
import { httpClient } from './httpClient.js';

export function getAllWorkshops(filters = {}) {
  return httpClient.get(API_ENDPOINTS.workshops.list, { query: filters });
}

export function getWorkshopById(id) {
  return httpClient.get(API_ENDPOINTS.workshops.detail(id));
}

export function createWorkshop(payload) {
  return httpClient.post(API_ENDPOINTS.admin.workshops.create, payload);
}

export function updateWorkshop(id, payload) {
  return httpClient.put(API_ENDPOINTS.admin.workshops.update(id), payload);
}

export function cancelWorkshop(id) {
  return httpClient.patch(API_ENDPOINTS.admin.workshops.cancel(id));
}
