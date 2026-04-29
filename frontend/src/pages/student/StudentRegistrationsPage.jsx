import { CalendarOutlined, QrcodeOutlined } from '@ant-design/icons';
import { Alert, Button, Card, List, Space, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import EmptyState from '../../components/common/EmptyState.jsx';
import LoadingState from '../../components/common/LoadingState.jsx';
import PageHeader from '../../components/common/PageHeader.jsx';
import StatusBadge from '../../components/common/StatusBadge.jsx';
import { getMyRegistrations } from '../../services/registrationService.js';
import { formatDate, formatMoney } from '../../utils/formatters.js';

export default function StudentRegistrationsPage() {
  const [registrations, setRegistrations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let ignore = false;

    async function loadRegistrations() {
      setLoading(true);
      setError('');

      try {
        const result = await getMyRegistrations();

        if (!ignore) {
          setRegistrations(result);
        }
      } catch (loadError) {
        if (!ignore) {
          setError(loadError.message);
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    loadRegistrations();

    return () => {
      ignore = true;
    };
  }, []);

  return (
    <div className="page-stack">
      <PageHeader
        title="Đăng ký của tôi"
        eyebrow="Student Portal"
        description="Theo dõi workshop đã đăng ký, trạng thái thanh toán và QR ticket."
      />

      {error && <Alert type="error" showIcon message={error} />}

      <Card bordered={false}>
        {loading ? (
          <LoadingState rows={8} card={false} />
        ) : registrations.length > 0 ? (
          <List
            itemLayout="vertical"
            dataSource={registrations}
            renderItem={(registration) => (
              <List.Item
                actions={[
                  <Link to={`/student/tickets/${registration.id}`} key="ticket">
                    <Button type="primary" icon={<QrcodeOutlined />}>
                      Xem QR ticket
                    </Button>
                  </Link>,
                ]}
              >
                <List.Item.Meta
                  title={registration.workshop?.title}
                  description={
                    <Space wrap>
                      <StatusBadge status={registration.status} type="registration" />
                      <StatusBadge status={registration.paymentStatus} type="payment" />
                    </Space>
                  }
                />
                <Space direction="vertical" size={4}>
                  <Typography.Text>
                    <CalendarOutlined /> {formatDate(registration.workshop?.date)} ·{' '}
                    {registration.workshop?.startTime}-{registration.workshop?.endTime}
                  </Typography.Text>
                  <Typography.Text type="secondary">
                    Phòng {registration.workshop?.room} · {formatMoney(registration.workshop?.price)}
                  </Typography.Text>
                </Space>
              </List.Item>
            )}
          />
        ) : (
          <EmptyState description="Bạn chưa đăng ký workshop nào." />
        )}
      </Card>
    </div>
  );
}
