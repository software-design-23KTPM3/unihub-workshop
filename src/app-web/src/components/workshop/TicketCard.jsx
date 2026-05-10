import {
  CalendarOutlined,
  CheckCircleOutlined,
  EnvironmentOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Card, Divider, Space, Typography } from 'antd';
import { useEffect, useState } from 'react';
import StatusBadge from '../common/StatusBadge.jsx';
import { formatDate } from '../../utils/formatters.js';
import { getRegistrationQrBlob } from '../../services/registrationService.js';

export default function TicketCard({ registration, currentUser }) {
  const workshop = registration?.workshop;
  const isValidTicket = registration.status === 'SUCCESS' || registration.status === 'CHECKED_IN';
  const [qrImageUrl, setQrImageUrl] = useState('');

  useEffect(() => {
    if (!isValidTicket || !registration?.id) {
      setQrImageUrl('');
      return undefined;
    }

    const controller = new AbortController();
    let objectUrl = '';

    getRegistrationQrBlob(registration.id, { signal: controller.signal })
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob);
        setQrImageUrl(objectUrl);
      })
      .catch((error) => {
        if (error.name !== 'AbortError') {
          setQrImageUrl('');
        }
      });

    return () => {
      controller.abort();
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [isValidTicket, registration?.id]);

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
          <StatusBadge status={isValidTicket ? 'VALID' : 'PENDING'} />
          <Typography.Paragraph className="ticket-card__hint" style={{ marginTop: 16 }}>
            {isValidTicket
              ? 'Vé hợp lệ. Xuất trình QR này tại cửa phòng để check-in.'
              : 'Đăng ký đang chờ thanh toán. QR sẽ có hiệu lực sau khi thanh toán thành công.'}
          </Typography.Paragraph>
        </div>
        <div className="ticket-card__qr">
          {isValidTicket && qrImageUrl ? (
            <img src={qrImageUrl} alt="Workshop check-in QR" width={148} height={148} />
          ) : isValidTicket ? (
            <Typography.Text type="secondary">Đang tải QR</Typography.Text>
          ) : (
            <Typography.Text type="secondary">QR chờ thanh toán</Typography.Text>
          )}
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
        {/* <Space direction="vertical" size={8}>
          <Typography.Text>
            <CheckCircleOutlined /> Mã vé: {registration.id}
          </Typography.Text>
          <StatusBadge status={registration.paymentStatus} type="payment" />
        </Space> */}
      </div>
    </Card>
  );
}
