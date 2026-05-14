import {
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  EnvironmentOutlined,
  QrcodeOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Button, Card, Progress, Space, Tag, Typography, message } from 'antd';
import dayjs from 'dayjs';
import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import StatusBadge from '../common/StatusBadge.jsx';
import { formatDate, formatDateTime, formatMoney } from '../../utils/formatters.js';
import { registerWorkshop } from '../../services/registrationService.js';

export default function WorkshopCard({ workshop, myRegistration, onRegistered }) {
  const navigate = useNavigate();
  const [registering, setRegistering] = useState(false);
  const registrationId = myRegistration?.id || myRegistration?.registrationId;
  const hasRegistered = Boolean(registrationId);
  const pendingPayment = myRegistration?.status === 'PENDING' || myRegistration?.paymentStatus === 'PENDING';
  const remainingSeats = Math.max(workshop.capacity - workshop.registeredCount, 0);
  const fillPercent = workshop.capacity
    ? Math.min(100, Math.round((workshop.registeredCount / workshop.capacity) * 100))
    : 0;
  const registrationStart = dayjs(workshop.registrationStartTime);
  const registrationEnd = dayjs(workshop.registrationEndTime);
  const now = dayjs();
  const isBeforeRegistration = registrationStart.isValid() && now.isBefore(registrationStart);
  const isAfterRegistration = registrationEnd.isValid() && now.isAfter(registrationEnd);
  const isUnavailable =
    workshop.status === 'FULL' ||
    workshop.status === 'CANCELLED' ||
    hasRegistered ||
    isBeforeRegistration ||
    isAfterRegistration;
  const registrationLabel = isBeforeRegistration
    ? `Mở đăng ký ${formatDateTime(workshop.registrationStartTime)}`
    : isAfterRegistration
      ? 'Đã hết hạn đăng ký'
      : `Đăng ký đến ${formatDateTime(workshop.registrationEndTime)}`;
  const registrationBadge = pendingPayment ? 'PAID_PENDING' : hasRegistered ? 'REGISTERED' : null;

  const handleQuickRegister = async () => {
    if (isUnavailable) {
      return;
    }

    setRegistering(true);
    try {
      const registration = await registerWorkshop(workshop.id);
      const registrationId = registration.registrationId || registration.id;
      onRegistered?.(workshop, registration);

      if (registration.status === 'PENDING') {
        message.info(registration.message || 'Đã giữ chỗ. Vui lòng hoàn tất thanh toán.');
        navigate(`/student/tickets/${registrationId}/payment`);
        return;
      }

      message.success(registration.message || 'Đăng ký workshop thành công!');
      navigate(`/student/tickets/${registrationId}`);
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
          {registrationBadge && <StatusBadge status={registrationBadge} />}
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
          <Typography.Text type="secondary">
            <ClockCircleOutlined /> {registrationLabel}
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
          {(workshop.tags || []).slice(0, 3).map((tag) => (
            <Tag key={tag}>{tag}</Tag>
          ))}
        </Space>

        <div className="workshop-card__actions">
          <Link to={`/student/workshops/${workshop.id}`}>
            <Button type="primary">Xem chi tiết</Button>
          </Link>
          {hasRegistered ? (
            <Button
              icon={pendingPayment ? <ClockCircleOutlined /> : <QrcodeOutlined />}
              onClick={() =>
                navigate(
                  pendingPayment
                    ? `/student/tickets/${registrationId}/payment`
                    : `/student/tickets/${registrationId}`,
                )
              }
            >
              {pendingPayment ? 'Thanh toán' : 'Xem vé'}
            </Button>
          ) : (
            <Button
              disabled={isUnavailable}
              loading={registering}
              onClick={handleQuickRegister}
            >
              {workshop.isPaid ? 'Giữ chỗ và thanh toán' : 'Đăng ký'}
            </Button>
          )}
        </div>

        {hasRegistered && (
          <Typography.Text
            className={`workshop-card__registered${pendingPayment ? ' workshop-card__registered--pending' : ''}`}
            type={pendingPayment ? 'warning' : 'success'}
          >
            <CheckCircleOutlined />{' '}
            {pendingPayment ? 'Bạn đã giữ chỗ, cần hoàn tất thanh toán.' : 'Bạn đã đăng ký workshop này.'}
          </Typography.Text>
        )}
      </Space>
    </Card>
  );
}
