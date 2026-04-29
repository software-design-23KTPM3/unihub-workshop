import { SafetyCertificateOutlined } from '@ant-design/icons';
import { Button, Card, Tag, Typography } from 'antd';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth.js';

const roleTargets = {
  STUDENT: '/student/workshops',
  ORGANIZER: '/admin/dashboard',
  STAFF: '/admin/dashboard',
};

export default function LoginPage() {
  const { currentUser, login, isLoading, error } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <div>Đang tải...</div>;
  }

  if (error) {
    return <div>Lỗi xác thực: {error.message}</div>;
  }

  if (currentUser) {
    const target = roleTargets[currentUser.role] || '/';
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
