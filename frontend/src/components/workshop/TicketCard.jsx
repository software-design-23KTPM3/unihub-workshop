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
            Thông tin tham dự
          </Typography.Text>
          <Typography.Title level={2}>{workshop.title}</Typography.Title>
          <StatusBadge status={registration.status === 'SUCCESS' ? 'VALID' : 'PENDING'} />
          <Typography.Paragraph className="ticket-card__hint" style={{ marginTop: 16 }}>
            {registration.status === 'SUCCESS' 
              ? 'Vé hợp lệ. Mã QR đã được gửi về email của bạn. Vui lòng xuất trình email tại quầy xác nhận.' 
              : 'Đăng ký đang chờ thanh toán. Sau khi thanh toán thành công, mã QR sẽ được gửi về email của bạn.'}
          </Typography.Paragraph>
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
          <StatusBadge status={registration.paymentStatus} type="payment" />
        </Space>
      </div>
    </Card>
  );
}
