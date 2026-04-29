import { mockRegistrations } from './registrations.mock.js';
import { mockWorkshops } from './workshops.mock.js';
import { cloneData } from '../utils/mockApi.js';

export const mockStore = {
  workshops: cloneData(mockWorkshops),
  registrations: cloneData(mockRegistrations),
};
