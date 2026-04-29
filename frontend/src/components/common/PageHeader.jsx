import { Space, Typography } from 'antd';

export default function PageHeader({ title, description, eyebrow, extra }) {
  return (
    <div className="page-header">
      <div>
        {eyebrow && <Typography.Text className="page-header__eyebrow">{eyebrow}</Typography.Text>}
        <Typography.Title level={2}>{title}</Typography.Title>
        {description && <Typography.Paragraph>{description}</Typography.Paragraph>}
      </div>
      {extra && <Space className="page-header__extra">{extra}</Space>}
    </div>
  );
}
