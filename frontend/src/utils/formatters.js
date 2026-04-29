import dayjs from 'dayjs';

export function formatDate(date) {
  return dayjs(date).format('DD/MM/YYYY');
}

export function formatDateTime(value) {
  return dayjs(value).format('DD/MM/YYYY HH:mm');
}

export function formatMoney(value) {
  const amount = Number(value || 0);

  if (amount <= 0) {
    return 'Miễn phí';
  }

  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount);
}
