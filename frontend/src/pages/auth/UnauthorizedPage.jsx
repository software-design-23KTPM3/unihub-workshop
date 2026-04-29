import { Button, Result } from 'antd';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth.js';

const roleTargets = {
  STUDENT: '/student/workshops',
  ORGANIZER: '/admin/dashboard',
};

export default function UnauthorizedPage() {
  const { currentUser } = useAuth();
  const target = roleTargets[currentUser?.role] || '/login';

  return (
    <Result
      status="403"
      title="Không có quyền truy cập"
      subTitle="Tài khoản hiện tại không được phép mở khu vực này."
      extra={
        <Link to={target}>
          <Button type="primary">Về trang phù hợp</Button>
        </Link>
      }
    />
  );
}
