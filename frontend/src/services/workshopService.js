import { API_ENDPOINTS } from './apiEndpoints.js';
import { httpClient } from './httpClient.js';

export function getAllWorkshops(filters = {}) {
  return httpClient.get(API_ENDPOINTS.workshops.list, { query: filters });
}

export function getWorkshopById(id) {
  return httpClient.get(API_ENDPOINTS.workshops.detail(id));
}

function toWorkshopRequest(payload) {
  const { pdfFile, ...workshop } = payload;
  return workshop;
}

function toWorkshopBody(payload) {
  if (!payload?.pdfFile) {
    return toWorkshopRequest(payload);
  }

  const body = new FormData();
  body.append(
    'workshop',
    new Blob([JSON.stringify(toWorkshopRequest(payload))], { type: 'application/json' }),
  );
  body.append('file', payload.pdfFile);
  return body;
}

export function createWorkshop(payload) {
  return httpClient.post(API_ENDPOINTS.admin.workshops.create, toWorkshopBody(payload));
}

export function updateWorkshop(id, payload) {
  return httpClient.put(API_ENDPOINTS.admin.workshops.update(id), toWorkshopBody(payload));
}

export function cancelWorkshop(id) {
  return httpClient.patch(API_ENDPOINTS.admin.workshops.cancel(id));
}
