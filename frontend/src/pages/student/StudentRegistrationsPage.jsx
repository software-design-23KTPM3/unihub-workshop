import { CalendarOutlined, ClockCircleOutlined, QrcodeOutlined, ReloadOutlined } from '@ant-design/icons';
import { Alert, Button, Card, List, Space, Typography } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import EmptyState from '../../components/common/EmptyState.jsx';
import LoadingState from '../../components/common/LoadingState.jsx';
import PageHeader from '../../components/common/PageHeader.jsx';
import StatusBadge from '../../components/common/StatusBadge.jsx';
import { getMyRegistrations } from '../../services/registrationService.js';
import { formatDate, formatMoney } from '../../utils/formatters.js';

export default function StudentRegistrationsPage() {
  const location = useLocation();
  const [registrations, setRegistrations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');

  const loadRegistrations = useCallback(async ({ showLoading = false } = {}) => {
    if (showLoading) {
      setLoading(true);
    } else {
      setRefreshing(true);
    }

    setError('');

    try {
      const result = await getMyRegistrations();
      setRegistrations(result);
      return result;
    } catch (loadError) {
      setError(loadError.message);
      return [];
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    let ignore = false;

    async function loadWithShortPolling() {
      const firstResult = await loadRegistrations({ showLoading: true });

      if (!location.state?.registrationAccepted || ignore) {
        return;
      }

      const registrationId = location.state.registrationId;
      const hasAcceptedRegistration = (items) =>
        items.some((item) => item.id === registrationId || item.registrationId === registrationId);

      if (!registrationId || hasAcceptedRegistration(firstResult)) {
        return;
      }

      for (let attempt = 0; attempt < 4; attempt += 1) {
        await new Promise((resolve) => {
          setTimeout(resolve, 1200);
        });

        if (ignore) {
          return;
        }

        const result = await loadRegistrations();
        if (hasAcceptedRegistration(result)) {
          return;
        }
      }
    }

    loadWithShortPolling();

    return () => {
      ignore = true;
    };
  }, [loadRegistrations, location.state]);

  return (
    <div className="page-stack">
      <PageHeader
        title="Đăng ký của tôi"
        eyebrow="Student Portal"
        description="Theo dõi workshop đã đăng ký và trạng thái thanh toán của bạn."
        extra={
          <Button icon={<ReloadOutlined />} loading={refreshing} onClick={() => loadRegistrations()}>
            Làm mới
          </Button>
        }
      />

      {location.state?.registrationAccepted && (
        <Alert
          type="info"
          showIcon
          message="Yêu cầu đăng ký đã được ghi nhận."
          description={location.state.message || 'Nếu đăng ký chưa xuất hiện ngay, vui lòng đợi vài giây rồi làm mới.'}
        />
      )}

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
                  registration.status === 'PENDING' && (
                    <Link to={`/student/tickets/${registration.id}`} key="pay">
                      <Button type="primary" style={{ backgroundColor: '#faad14', borderColor: '#faad14' }}>
                        Xem thanh toán
                      </Button>
                    </Link>
                  ),
                  <Link to={`/student/tickets/${registration.id}`} key="ticket">
                    <Button icon={<QrcodeOutlined />}>
                      Xem chi tiết vé
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
                  {registration.status === 'PENDING' && (
                    <Typography.Text type="warning">
                      <ClockCircleOutlined /> Chờ thanh toán. Chỗ của bạn đang được giữ tạm thời.
                    </Typography.Text>
                  )}
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
