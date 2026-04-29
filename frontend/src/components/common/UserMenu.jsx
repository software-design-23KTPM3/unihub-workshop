import { LogoutOutlined, UserOutlined } from '@ant-design/icons';
import { Avatar, Dropdown, Space, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth.js';
import RoleTag from './RoleTag.jsx';

export default function UserMenu() {
  const navigate = useNavigate();
  const { currentUser, logout } = useAuth();

  const items = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Đăng xuất',
      onClick: () => {
        logout();
        navigate('/login', { replace: true });
      },
    },
  ];

  return (
    <Dropdown menu={{ items }} placement="bottomRight">
      <Space className="user-menu">
        <Avatar icon={<UserOutlined />} />
        <span className="user-menu__text">
          <Typography.Text strong>{currentUser?.name || 'Guest'}</Typography.Text>
          <RoleTag role={currentUser?.role} />
        </span>
      </Space>
    </Dropdown>
  );
}
