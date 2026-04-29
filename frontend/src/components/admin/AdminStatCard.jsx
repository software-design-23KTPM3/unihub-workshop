import { Card, Typography } from 'antd';

export default function AdminStatCard({ title, value, icon, tone = 'blue', suffix }) {
  return (
    <Card className={`admin-stat-card admin-stat-card--${tone}`} bordered={false}>
      <div className="admin-stat-card__top">
        <Typography.Text type="secondary">{title}</Typography.Text>
        <span className="admin-stat-card__icon">{icon}</span>
      </div>
      <div className="admin-stat-card__value">
        {value}
        {suffix && <span>{suffix}</span>}
      </div>
    </Card>
  );
}
