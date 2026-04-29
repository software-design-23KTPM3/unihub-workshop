import { USE_MOCK } from '../config/api.js';
import { mockStore } from '../mocks/mockStore.js';
import { getCurrentUser } from './authService.js';
import { createId, rejectMock, resolveMock } from '../utils/mockApi.js';
import { API_ENDPOINTS } from './apiEndpoints.js';
import { httpClient } from './httpClient.js';

const ACTIVE_REGISTRATION_STATUSES = ['REGISTERED', 'PAID_PENDING', 'PAID'];
const studentsById = {
  2312345: {
    studentName: 'Sinh viên UniHub',
    studentEmail: 'student@example.com',
  },
  2312346: {
    studentName: 'Tran Thi B',
    studentEmail: 'student2@unihub.edu.vn',
  },
  2312347: {
    studentName: 'Pham Gia Han',
    studentEmail: 'giahan@unihub.edu.vn',
  },
};

function getStudentId() {
  const currentUser = getCurrentUser();
  return currentUser?.studentId || currentUser?.id;
}

function enrichRegistration(registration) {
  const workshop = mockStore.workshops.find((item) => item.id === registration.workshopId);
  const student = studentsById[registration.studentId] || {
    studentName: `Sinh viên ${registration.studentId}`,
    studentEmail: `${registration.studentId}@unihub.edu.vn`,
  };

  return {
    ...registration,
    workshop,
    ...student,
  };
}

function applyRegistrationFilters(registrations, filters = {}) {
  return registrations.filter((registration) => {
    if (filters.workshopId && registration.workshopId !== filters.workshopId) {
      return false;
    }

    if (filters.status && registration.status !== filters.status) {
      return false;
    }

    return true;
  });
}

export function registerWorkshop(workshopId) {
  if (!USE_MOCK) {
    return httpClient.post(API_ENDPOINTS.registrations.create, { workshopId });
  }

  const studentId = getStudentId();

  if (!studentId) {
    return rejectMock('Bạn cần đăng nhập bằng tài khoản sinh viên để đăng ký.');
  }

  const workshop = mockStore.workshops.find((item) => item.id === workshopId);

  if (!workshop) {
    return rejectMock('Workshop không tồn tại.');
  }

  if (workshop.status === 'CANCELLED') {
    return rejectMock('Workshop đã bị hủy.');
  }

  if (workshop.status === 'FULL' || workshop.registeredCount >= workshop.capacity) {
    return rejectMock('Workshop đã hết chỗ.');
  }

  const existingRegistration = mockStore.registrations.find(
    (item) =>
      item.studentId === studentId &&
      item.workshopId === workshopId &&
      ACTIVE_REGISTRATION_STATUSES.includes(item.status),
  );

  if (existingRegistration) {
    return rejectMock('Bạn đã đăng ký workshop này.');
  }

  const registration = {
    id: createId('reg'),
    studentId,
    workshopId,
    status: workshop.isPaid ? 'PAID_PENDING' : 'REGISTERED',
    qrCode: `UNIHUB-${studentId}-${workshopId}-${Date.now()}`,
    registeredAt: new Date().toISOString(),
    paymentStatus: workshop.isPaid ? 'PENDING' : 'FREE',
  };

  mockStore.registrations.unshift(registration);
  workshop.registeredCount += 1;

  if (workshop.registeredCount >= workshop.capacity) {
    workshop.status = 'FULL';
  }

  return resolveMock(enrichRegistration(registration));
}

export function completeMockPayment(registrationId) {
  if (!USE_MOCK) {
    return httpClient.post(API_ENDPOINTS.registrations.mockPaymentSuccess(registrationId));
  }

  const registration = mockStore.registrations.find((item) => item.id === registrationId);

  if (!registration) {
    return rejectMock('Đăng ký không tồn tại.');
  }

  registration.status = 'PAID';
  registration.paymentStatus = 'PAID';

  return resolveMock(enrichRegistration(registration));
}

export function getMyRegistrations() {
  if (!USE_MOCK) {
    return httpClient.get(API_ENDPOINTS.registrations.mine);
  }

  const studentId = getStudentId();

  if (!studentId) {
    return resolveMock([]);
  }

  const registrations = mockStore.registrations
    .filter((item) => item.studentId === studentId)
    .map(enrichRegistration)
    .sort((first, second) => second.registeredAt.localeCompare(first.registeredAt));

  return resolveMock(registrations);
}

export function getAllRegistrations(filters = {}) {
  if (!USE_MOCK) {
    return httpClient.get(API_ENDPOINTS.admin.registrations, { query: filters });
  }

  const registrations = applyRegistrationFilters(mockStore.registrations, filters)
    .map(enrichRegistration)
    .sort((first, second) => second.registeredAt.localeCompare(first.registeredAt));

  return resolveMock(registrations);
}

export function getRegistrationById(id) {
  if (!USE_MOCK) {
    return httpClient.get(API_ENDPOINTS.registrations.detail(id));
  }

  const registration = mockStore.registrations.find((item) => item.id === id);

  if (!registration) {
    return rejectMock('Đăng ký không tồn tại.');
  }

  return resolveMock(enrichRegistration(registration));
}
