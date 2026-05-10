import { Tag } from 'antd';

const roleConfig = {
  STUDENT: { color: 'blue', label: 'Sinh viên' },
  ORGANIZER: { color: 'purple', label: 'Ban tổ chức' },
};

export default function RoleTag({ role }) {
  const config = roleConfig[role] || { color: 'default', label: role || 'Khách' };
  return <Tag className="role-tag" color={config.color}>{config.label}</Tag>;
}
