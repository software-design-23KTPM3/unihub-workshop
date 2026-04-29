const MIN_DELAY_MS = 300;
const MAX_DELAY_MS = 500;

export function cloneData(value) {
  if (typeof structuredClone === 'function') {
    return structuredClone(value);
  }

  return JSON.parse(JSON.stringify(value));
}

export function createId(prefix) {
  const randomPart =
    globalThis.crypto?.randomUUID?.() || Math.random().toString(36).slice(2, 12);
  return `${prefix}-${randomPart}`;
}

export function delay(ms) {
  return new Promise((resolve) => {
    globalThis.setTimeout(resolve, ms);
  });
}

export async function resolveMock(value) {
  const duration = MIN_DELAY_MS + Math.floor(Math.random() * (MAX_DELAY_MS - MIN_DELAY_MS + 1));
  await delay(duration);
  return cloneData(value);
}

export async function rejectMock(message) {
  const duration = MIN_DELAY_MS + Math.floor(Math.random() * (MAX_DELAY_MS - MIN_DELAY_MS + 1));
  await delay(duration);
  throw new Error(message);
}
