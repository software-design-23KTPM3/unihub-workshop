import { Tag } from 'antd';

const statusConfig = {
  ACTIVE: { color: 'blue', label: 'Đang mở' },
  CANCELLED: { color: 'red', label: 'Đã hủy' },
};

export default function WorkshopStatusTag({ status }) {
  const config = statusConfig[status] || { color: 'default', label: status };
  return <Tag color={config.color}>{config.label}</Tag>;
}
