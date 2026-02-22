export const ORDER_STATUS = {
  PENDING: 'PENDING',
  CONFIRMED: 'CONFIRMED',
  PROCESSING: 'PROCESSING',
  SHIPPED: 'SHIPPED',
  DELIVERED: 'DELIVERED',
  CANCELLED: 'CANCELLED',
};

export const ORDER_STATUS_LABELS = {
  PENDING: 'Đang chờ',
  CONFIRMED: 'Đã xác nhận',
  PROCESSING: 'Đang xử lý',
  SHIPPED: 'Đang giao',
  DELIVERED: 'Đã giao',
  CANCELLED: 'Đã hủy',
};

export const ORDER_STATUS_COLORS = {
  PENDING: 'yellow',
  CONFIRMED: 'blue',
  PROCESSING: 'indigo',
  SHIPPED: 'purple',
  DELIVERED: 'green',
  CANCELLED: 'red',
};

export const PAYMENT_METHODS = {
  COD: 'COD',
  CARD: 'CARD',
  BANKING: 'BANKING',
};

export const PAYMENT_METHOD_LABELS = {
  COD: 'Thanh toán khi nhận hàng',
  CARD: 'Thẻ tín dụng/ghi nợ',
  BANKING: 'Chuyển khoản ngân hàng',
};