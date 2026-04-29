import { LockOutlined, MailOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Form, Input, Space, Tag, Typography } from 'antd';
import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { getDemoAccounts } from '../../services/authService.js';
import { useAuth } from '../../hooks/useAuth.js';

const roleTargets = {
  STUDENT: '/student/workshops',
  ORGANIZER: '/admin/dashboard',
};

const roleLabels = {
  STUDENT: 'Sinh viên',
  ORGANIZER: 'Ban tổ chức',
};

export default function LoginPage() {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const location = useLocation();
  const { currentUser, login } = useAuth();
  const [error, setError] = useState('');
  const demoAccounts = getDemoAccounts();

  if (currentUser) {
    return <Navigate to={roleTargets[currentUser.role] || '/'} replace />;
  }

  const redirectAfterLogin = (user) => {
    const from = location.state?.from?.pathname;
    const target = roleTargets[user.role] || '/';
    const safeFrom = from?.startsWith(target.split('/').slice(0, 2).join('/')) ? from : null;
    navigate(safeFrom || target, { replace: true });
  };

  const handleSubmit = async (values) => {
    try {
      const user = await login(values.email, values.password);
      redirectAfterLogin(user);
    } catch (loginError) {
      setError(loginError.message);
    }
  };

  const handleDemoLogin = async (user) => {
    try {
      const loggedInUser = await login(user.email, '123456');
      redirectAfterLogin(loggedInUser);
    } catch (loginError) {
      setError(loginError.message);
    }
  };

  return (
    <div className="login-grid">
      <section className="login-intro">
        <Tag color="blue" icon={<SafetyCertificateOutlined />}>
          Xác thực demo
        </Tag>
        <Typography.Title>UniHub Workshop</Typography.Title>
        <Typography.Paragraph>
          Nền tảng quản lý tuần lễ kỹ năng và nghề nghiệp.
        </Typography.Paragraph>
      </section>

      <Card className="auth-card" bordered={false}>
        <Typography.Title level={2}>Đăng nhập</Typography.Title>
        <Typography.Paragraph>
          Sử dụng tài khoản demo hoặc nhập email và mật khẩu để kiểm tra phân quyền.
        </Typography.Paragraph>

        {error && (
          <Alert className="login-error" type="error" showIcon message={error} />
        )}

        <Form form={form} layout="vertical" onFinish={handleSubmit} requiredMark={false}>
          <Form.Item
            label="Email"
            name="email"
            rules={[
              { required: true, message: 'Vui lòng nhập email.' },
              { type: 'email', message: 'Email không hợp lệ.' },
            ]}
          >
            <Input size="large" prefix={<MailOutlined />} placeholder="student@example.com" />
          </Form.Item>

          <Form.Item
            label="Mật khẩu"
            name="password"
            rules={[{ required: true, message: 'Vui lòng nhập mật khẩu.' }]}
          >
            <Input.Password size="large" prefix={<LockOutlined />} placeholder="123456" />
          </Form.Item>

          <Form.Item>
            <Button type="primary" size="large" htmlType="submit" block>
              Đăng nhập
            </Button>
          </Form.Item>
        </Form>

        <Alert
          className="demo-account-alert"
          type="info"
          showIcon
          message="Tài khoản demo"
          description="Tất cả tài khoản demo dùng mật khẩu 123456."
        />

        <Space direction="vertical" size="small" className="demo-account-list">
          {demoAccounts.map((user) => (
            <div className="demo-account" key={user.email}>
              <div>
                <Typography.Text strong>{roleLabels[user.role]}</Typography.Text>
                <Typography.Text type="secondary">{user.email}</Typography.Text>
              </div>
              <Button onClick={() => handleDemoLogin(user)}>Đăng nhập</Button>
            </div>
          ))}
        </Space>
      </Card>
    </div>
  );
}
