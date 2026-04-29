import {
  CalendarOutlined,
  CheckCircleOutlined,
  EnvironmentOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Card, Divider, Space, Typography } from 'antd';
import { QRCodeCanvas } from 'qrcode.react';
import StatusBadge from '../common/StatusBadge.jsx';
import { formatDate } from '../../utils/formatters.js';

export default function TicketCard({ registration, currentUser }) {
  const workshop = registration?.workshop;

  if (!registration || !workshop) {
    return null;
  }

  return (
    <Card className="ticket-card" bordered={false}>
      <div className="ticket-card__top">
        <div>
          <Typography.Text className="ticket-card__eyebrow">
            UniHub Workshop Ticket
          </Typography.Text>
          <Typography.Title level={2}>{workshop.title}</Typography.Title>
          <StatusBadge status="VALID" />
          <Typography.Paragraph className="ticket-card__hint">
            Xuất trình QR tại quầy xác nhận trước giờ bắt đầu workshop.
          </Typography.Paragraph>
        </div>
        <div className="ticket-card__qr">
          <QRCodeCanvas value={registration.qrCode} size={168} />
        </div>
      </div>

      <Divider />

      <div className="ticket-card__details">
        <Space direction="vertical" size={8}>
          <Typography.Text>
            <UserOutlined /> {currentUser?.name || registration.studentId}
          </Typography.Text>
          <Typography.Text type="secondary">Mã sinh viên: {registration.studentId}</Typography.Text>
          <Typography.Text>
            <CalendarOutlined /> {formatDate(workshop.date)} · {workshop.startTime}-{workshop.endTime}
          </Typography.Text>
          <Typography.Text>
            <EnvironmentOutlined /> {workshop.room}
          </Typography.Text>
        </Space>
        <Space direction="vertical" size={8}>
          <Typography.Text>
            <CheckCircleOutlined /> Mã vé: {registration.id}
          </Typography.Text>
          <Typography.Text type="secondary">QR: {registration.qrCode}</Typography.Text>
          <StatusBadge status={registration.paymentStatus} type="payment" />
        </Space>
      </div>
    </Card>
  );
}
