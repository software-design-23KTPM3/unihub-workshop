import {
  BarChartOutlined,
  CalendarOutlined,
  DashboardOutlined,
  PlusCircleOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { Layout, Menu } from 'antd';
import { Link, Outlet, useLocation } from 'react-router-dom';
import AppLogo from '../components/common/AppLogo.jsx';
import UserMenu from '../components/common/UserMenu.jsx';

const { Header, Sider, Content } = Layout;

export default function AdminLayout() {
  const location = useLocation();
  const selectedKey = location.pathname.includes('/admin/workshops/create')
    ? '/admin/workshops/create'
    : location.pathname.includes('/admin/workshops')
      ? '/admin/workshops'
      : location.pathname;

  return (
    <Layout className="site-shell admin-shell">
      <Sider width={260} breakpoint="lg" collapsedWidth="0" className="admin-sider">
        <div className="admin-sider__brand">
          <AppLogo inverted />
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={[
            {
              key: '/admin/dashboard',
              icon: <DashboardOutlined />,
              label: <Link to="/admin/dashboard">Dashboard</Link>,
            },
            {
              key: '/admin/workshops',
              icon: <CalendarOutlined />,
              label: <Link to="/admin/workshops">Quản lý workshop</Link>,
            },
            {
              key: '/admin/workshops/create',
              icon: <PlusCircleOutlined />,
              label: <Link to="/admin/workshops/create">Tạo workshop</Link>,
            },
            {
              key: '/admin/registrations',
              icon: <TeamOutlined />,
              label: <Link to="/admin/registrations">Danh sách đăng ký</Link>,
            },
            {
              key: '/admin/statistics',
              icon: <BarChartOutlined />,
              label: <Link to="/admin/statistics">Thống kê</Link>,
            },
          ]}
        />
      </Sider>
      <Layout>
        <Header className="admin-header">
          <div className="admin-header__title">UniHub Workshop Admin Portal</div>
          <UserMenu />
        </Header>
        <Content className="admin-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
