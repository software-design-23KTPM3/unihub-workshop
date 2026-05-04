import {
  CalendarOutlined,
  EnvironmentOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Button, Card, Progress, Space, Tag, Typography, message } from 'antd';
import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import StatusBadge from '../common/StatusBadge.jsx';
import { formatDate, formatMoney } from '../../utils/formatters.js';
import { registerWorkshop } from '../../services/registrationService.js';

export default function WorkshopCard({ workshop }) {
  const navigate = useNavigate();
  const [registering, setRegistering] = useState(false);
  const remainingSeats = Math.max(workshop.capacity - workshop.registeredCount, 0);
  const fillPercent = Math.round((workshop.registeredCount / workshop.capacity) * 100);
  const isUnavailable = workshop.status === 'FULL' || workshop.status === 'CANCELLED';

  const handleQuickRegister = async () => {
    if (workshop.isPaid) {
      // Redirect to detail page for paid workshops to handle payment
      navigate(`/student/workshops/${workshop.id}`);
      return;
    }

    setRegistering(true);
    try {
      const registration = await registerWorkshop(workshop.id);
      message.success('Đăng ký workshop thành công!');
      navigate(`/student/tickets/${registration.registrationId || registration.id}`);
    } catch (err) {
      message.error(err.message || 'Đăng ký thất bại');
    } finally {
      setRegistering(false);
    }
  };

  return (
    <Card className="workshop-card" bordered={false}>
      <Space direction="vertical" size="middle" className="workshop-card__body">
        <Space wrap>
          <Tag color="geekblue">{workshop.topic}</Tag>
          <StatusBadge status={workshop.status} />
          <StatusBadge status={workshop.isPaid ? 'PAID_EVENT' : 'FREE'} type="price" />
        </Space>

        <div>
          <Typography.Title level={4}>{workshop.title}</Typography.Title>
          <Typography.Paragraph className="workshop-card__description">
            {workshop.description}
          </Typography.Paragraph>
        </div>

        <Space direction="vertical" size={6}>
          <Typography.Text>
            <UserOutlined /> {workshop.speakerName}
          </Typography.Text>
          <Typography.Text>
            <CalendarOutlined /> {formatDate(workshop.date)} · {workshop.startTime}-{workshop.endTime}
          </Typography.Text>
          <Typography.Text>
            <EnvironmentOutlined /> {workshop.room}
          </Typography.Text>
        </Space>

        <div className="seat-progress">
          <div className="seat-progress__label">
            <Typography.Text>
              <TeamOutlined /> Còn {remainingSeats}/{workshop.capacity} chỗ
            </Typography.Text>
            <Typography.Text strong>{formatMoney(workshop.price)}</Typography.Text>
          </div>
          <Progress percent={fillPercent} size="small" showInfo={false} />
        </div>

        <Space wrap>
          {workshop.tags.slice(0, 3).map((tag) => (
            <Tag key={tag}>{tag}</Tag>
          ))}
        </Space>

        <div className="workshop-card__actions">
          <Link to={`/student/workshops/${workshop.id}`}>
            <Button type="primary">Xem chi tiết</Button>
          </Link>
          <Button 
            disabled={isUnavailable}
            loading={registering}
            onClick={handleQuickRegister}
          >
            Đăng ký
          </Button>
        </div>
      </Space>
    </Card>
  );
}
