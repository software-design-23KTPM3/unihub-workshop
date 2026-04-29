import { USE_MOCK } from '../config/api.js';
import { mockStore } from '../mocks/mockStore.js';
import { createId, rejectMock, resolveMock } from '../utils/mockApi.js';
import { API_ENDPOINTS } from './apiEndpoints.js';
import { httpClient } from './httpClient.js';

function getNormalizedText(value) {
  return String(value || '').trim().toLowerCase();
}

function applyWorkshopFilters(workshops, filters = {}) {
  const search = getNormalizedText(filters.search || filters.keyword);
  const topic = getNormalizedText(filters.topic);
  const room = getNormalizedText(filters.room);
  const status = getNormalizedText(filters.status);
  const tag = getNormalizedText(filters.tag);

  return workshops
    .filter((workshop) => {
      if (search) {
        const haystack = [
          workshop.title,
          workshop.speakerName,
          workshop.topic,
          workshop.description,
          workshop.tags.join(' '),
        ]
          .join(' ')
          .toLowerCase();

        if (!haystack.includes(search)) {
          return false;
        }
      }

      if (topic && workshop.topic.toLowerCase() !== topic) {
        return false;
      }

      if (room && !workshop.room.toLowerCase().includes(room)) {
        return false;
      }

      if (status && workshop.status.toLowerCase() !== status) {
        return false;
      }

      if (tag && !workshop.tags.some((item) => item.toLowerCase() === tag)) {
        return false;
      }

      if (typeof filters.isPaid === 'boolean' && workshop.isPaid !== filters.isPaid) {
        return false;
      }

      if (filters.date && workshop.date !== filters.date) {
        return false;
      }

      return true;
    })
    .sort((first, second) =>
      `${first.date} ${first.startTime}`.localeCompare(`${second.date} ${second.startTime}`),
    );
}

export function getAllWorkshops(filters = {}) {
  if (!USE_MOCK) {
    return httpClient.get(API_ENDPOINTS.workshops.list, { query: filters });
  }

  return resolveMock(applyWorkshopFilters(mockStore.workshops, filters));
}

export function getWorkshopById(id) {
  if (!USE_MOCK) {
    return httpClient.get(API_ENDPOINTS.workshops.detail(id));
  }

  const workshop = mockStore.workshops.find((item) => item.id === id);

  if (!workshop) {
    return rejectMock('Workshop không tồn tại.');
  }

  return resolveMock(workshop);
}

export function createWorkshop(payload) {
  if (!USE_MOCK) {
    return httpClient.post(API_ENDPOINTS.admin.workshops.create, payload);
  }

  const capacity = Number(payload.capacity || 0);
  const price = Number(payload.price || 0);
  const workshop = {
    id: createId('ws'),
    title: payload.title,
    speakerName: payload.speakerName || '',
    speakerTitle: payload.speakerTitle || '',
    topic: payload.topic || 'General',
    description: payload.description || '',
    aiSummary: payload.aiSummary || '',
    room: payload.room || '',
    roomMapUrl: payload.roomMapUrl || '',
    roomMapText: payload.roomMapText || '',
    date: payload.date,
    startTime: payload.startTime,
    endTime: payload.endTime,
    capacity,
    registeredCount: 0,
    price,
    status: payload.status || 'OPEN',
    tags: Array.isArray(payload.tags) ? payload.tags : [],
    isPaid: Boolean(payload.isPaid || price > 0),
  };

  mockStore.workshops.unshift(workshop);
  return resolveMock(workshop);
}

export function updateWorkshop(id, payload) {
  if (!USE_MOCK) {
    return httpClient.put(API_ENDPOINTS.admin.workshops.update(id), payload);
  }

  const index = mockStore.workshops.findIndex((item) => item.id === id);

  if (index === -1) {
    return rejectMock('Workshop không tồn tại.');
  }

  const current = mockStore.workshops[index];
  const nextCapacity =
    payload.capacity !== undefined ? Number(payload.capacity) : current.capacity;

  if (nextCapacity < current.registeredCount) {
    return rejectMock('Sức chứa không thể nhỏ hơn số sinh viên đã đăng ký.');
  }

  const updated = {
    ...current,
    ...payload,
    capacity: nextCapacity,
    price: payload.price !== undefined ? Number(payload.price) : current.price,
    tags: Array.isArray(payload.tags) ? payload.tags : current.tags,
  };

  updated.isPaid = Boolean(updated.isPaid || updated.price > 0);
  updated.status =
    updated.status === 'CANCELLED'
      ? 'CANCELLED'
      : updated.registeredCount >= updated.capacity
        ? 'FULL'
        : 'OPEN';

  mockStore.workshops[index] = updated;
  return resolveMock(updated);
}

export function cancelWorkshop(id) {
  if (!USE_MOCK) {
    return httpClient.patch(API_ENDPOINTS.admin.workshops.cancel(id));
  }

  const index = mockStore.workshops.findIndex((item) => item.id === id);

  if (index === -1) {
    return rejectMock('Workshop không tồn tại.');
  }

  mockStore.workshops[index] = {
    ...mockStore.workshops[index],
    status: 'CANCELLED',
  };

  return resolveMock(mockStore.workshops[index]);
}
