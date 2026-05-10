import { Button, Result } from 'antd';
import { Link } from 'react-router-dom';

export default function ErrorState({ title = 'Không tải được dữ liệu', message, actionTo, actionText }) {
  return (
    <Result
      className="state-card"
      status="warning"
      title={title}
      subTitle={message}
      extra={
        actionTo && (
          <Link to={actionTo}>
            <Button type="primary">{actionText || 'Quay lại'}</Button>
          </Link>
        )
      }
    />
  );
}
