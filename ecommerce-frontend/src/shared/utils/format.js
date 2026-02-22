import { format } from 'date-fns';
import { vi } from 'date-fns/locale';

export const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(amount);
};

export const formatDate = (date, formatStr = 'dd/MM/yyyy') => {
  return format(new Date(date), formatStr, { locale: vi });
};

export const formatDateTime = (date) => {
  return format(new Date(date), 'dd/MM/yyyy HH:mm', { locale: vi });
};

export const formatNumber = (number) => {
  return new Intl.NumberFormat('vi-VN').format(number);
};

export const slugify = (text) => {
  return text
    .toString()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[đĐ]/g, 'd')
    .replace(/[^a-z0-9 -]/g, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .trim();
};