import { SafetyCertificateOutlined } from '@ant-design/icons';
import { Button, Card, Tag, Typography } from 'antd';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth.js';

const roleTargets = {
  STUDENT: '/student/workshops',
  ORGANIZER: '/admin/dashboard',
  STAFF: '/admin/dashboard',
};

function isAllowedReturnPath(path, role) {
  if (!path) {
    return false;
  }
  if (path.startsWith('/student')) {
    return role === 'STUDENT';
  }
  if (path.startsWith('/admin')) {
    return role === 'ORGANIZER' || role === 'STAFF';
  }
  return path === '/';
}

export default function LoginPage() {
  const { currentUser, login, isAuthenticated, isLoading, error } = useAuth();
  const location = useLocation();
  const from = location.state?.from?.pathname;
  
  if (isLoading) {
    return (
      <div className="login-loading">
        <Card bordered={false} className="auth-card">
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Typography.Title level={3}>Đang xác thực...</Typography.Title>
            <Typography.Paragraph>Vui lòng đợi trong giây lát khi hệ thống xử lý.</Typography.Paragraph>
          </div>
        </Card>
      </div>
    );
  }

  if (error) {
    return (
      <div className="login-error">
        <Card bordered={false} className="auth-card">
          <Typography.Title level={3} type="danger">Lỗi xác thực</Typography.Title>
          <Typography.Paragraph>{error.message}</Typography.Paragraph>
          <Button onClick={() => window.location.href = '/login'}>Thử lại</Button>
        </Card>
      </div>
    );
  }

  if (isAuthenticated && currentUser) {
    const target = isAllowedReturnPath(from, currentUser.role)
      ? from
      : roleTargets[currentUser.role] || '/';
    return <Navigate to={target} replace />;
  }

  return (
    <div className="login-grid">
      <section className="login-intro">
        <Tag color="blue" icon={<SafetyCertificateOutlined />}>
          Bảo mật Keycloak OIDC
        </Tag>
        <Typography.Title>UniHub Workshop</Typography.Title>
        <Typography.Paragraph>
          Nền tảng quản lý tuần lễ kỹ năng và nghề nghiệp.
        </Typography.Paragraph>
      </section>

      <Card className="auth-card" bordered={false}>
        <Typography.Title level={2}>Đăng nhập</Typography.Title>
        <Typography.Paragraph>
          Vui lòng đăng nhập thông qua hệ thống phân quyền tập trung Keycloak.
        </Typography.Paragraph>

        <Button type="primary" size="large" onClick={() => login()} block>
          Đăng nhập với UniHub ID
        </Button>
      </Card>
    </div>
  );
}
