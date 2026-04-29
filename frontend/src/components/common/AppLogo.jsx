import { BankOutlined } from '@ant-design/icons';
import { Typography } from 'antd';

export default function AppLogo({ compact = false, inverted = false }) {
  return (
    <div className={`app-logo ${inverted ? 'app-logo--inverted' : ''}`}>
      <span className="app-logo__icon">
        <BankOutlined />
      </span>
      {!compact && (
        <span className="app-logo__copy">
          <Typography.Text strong className="app-logo__name">
            UniHub Workshop
          </Typography.Text>
          <Typography.Text className="app-logo__subtitle">
            Career Week Platform
          </Typography.Text>
        </span>
      )}
    </div>
  );
}
