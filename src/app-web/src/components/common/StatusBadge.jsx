import { Tag } from 'antd';

const configs = {
  OPEN: { color: 'blue', label: 'Đang mở' },
  FULL: { color: 'orange', label: 'Đã đầy' },
  CANCELLED: { color: 'red', label: 'Đã hủy' },
  FREE: { color: 'green', label: 'Miễn phí' },
  PAID_EVENT: { color: 'purple', label: 'Có phí' },
  REGISTERED: { color: 'blue', label: 'Đã đăng ký' },
  PAID_PENDING: { color: 'gold', label: 'Chờ thanh toán' },
  PAID: { color: 'green', label: 'Đã thanh toán' },
  PENDING: { color: 'gold', label: 'Đang chờ' },
  SUCCESS: { color: 'green', label: 'Thành công' },
  CHECKED_IN: { color: 'cyan', label: 'Đã check-in' },
  FAILED: { color: 'red', label: 'Thất bại' },
  CANCELLED_REG: { color: 'red', label: 'Đã hủy' },
  FREE_PAYMENT: { color: 'green', label: 'Miễn phí' },
  VALID: { color: 'success', label: 'Vé hợp lệ' },
};

export default function StatusBadge({ status, type }) {
  const key =
    type === 'price'
      ? status
      : type === 'registration' && status === 'CANCELLED'
        ? 'CANCELLED_REG'
        : type === 'payment' && status === 'FREE'
          ? 'FREE_PAYMENT'
          : status;
  const config = configs[key] || { color: 'default', label: status };

  return <Tag className="status-badge" color={config.color}>{config.label}</Tag>;
}
