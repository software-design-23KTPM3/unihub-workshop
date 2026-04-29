import { CalendarOutlined, QrcodeOutlined } from '@ant-design/icons';
import { Layout, Menu } from 'antd';
import { Link, Outlet, useLocation } from 'react-router-dom';
import AppLogo from '../components/common/AppLogo.jsx';
import UserMenu from '../components/common/UserMenu.jsx';

const { Header, Content } = Layout;

export default function StudentLayout() {
  const location = useLocation();
  const selectedKey = location.pathname.startsWith('/student/my-registrations')
    ? '/student/my-registrations'
    : '/student/workshops';

  return (
    <Layout className="site-shell student-shell">
      <Header className="student-header">
        <Link to="/student/workshops" className="header-brand">
          <AppLogo />
        </Link>
        <Menu
          mode="horizontal"
          selectedKeys={[selectedKey]}
          className="student-header__menu"
          items={[
            {
              key: '/student/workshops',
              icon: <CalendarOutlined />,
              label: <Link to="/student/workshops">Workshop</Link>,
            },
            {
              key: '/student/my-registrations',
              icon: <QrcodeOutlined />,
              label: <Link to="/student/my-registrations">Đăng ký của tôi</Link>,
            },
          ]}
        />
        <UserMenu />
      </Header>
      <Content className="student-content">
        <Outlet />
      </Content>
    </Layout>
  );
}
