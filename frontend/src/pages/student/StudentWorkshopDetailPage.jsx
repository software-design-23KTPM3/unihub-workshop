import {
  ArrowLeftOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  EnvironmentOutlined,
  ReadOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Modal,
  Result,
  Row,
  Space,
  Statistic,
  Typography,
} from 'antd';
import dayjs from 'dayjs';
import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import ErrorState from '../../components/common/ErrorState.jsx';
import LoadingState from '../../components/common/LoadingState.jsx';
import StatusBadge from '../../components/common/StatusBadge.jsx';
import { getMyRegistrations, registerWorkshop } from '../../services/registrationService.js';
import { getWorkshopById } from '../../services/workshopService.js';
import { formatDate, formatDateTime, formatMoney } from '../../utils/formatters.js';

export default function StudentWorkshopDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [workshop, setWorkshop] = useState(null);
  const [myRegistration, setMyRegistration] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [successRegistration, setSuccessRegistration] = useState(null);
  const [pendingRegistration, setPendingRegistration] = useState(null);
  const pendingPayment = myRegistration?.status === 'PENDING' || myRegistration?.paymentStatus === 'PENDING';

  useEffect(() => {
    let ignore = false;

    async function loadDetail() {
      setLoading(true);
      setError('');

      try {
        const [workshopResult, registrations] = await Promise.all([
          getWorkshopById(id),
          getMyRegistrations(),
        ]);

        if (!ignore) {
          setWorkshop(workshopResult);
          setMyRegistration(
            registrations.find(
              (registration) =>
                (registration.workshopId === id || registration.workshop?.id === id) &&
                registration.status !== 'CANCELLED',
            ) || null,
          );
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

    loadDetail();

    return () => {
      ignore = true;
    };
  }, [id]);

  const remainingSeats = useMemo(() => {
    if (!workshop) {
      return 0;
    }

    return Math.max(workshop.capacity - workshop.registeredCount, 0);
  }, [workshop]);

  const unavailableReason = useMemo(() => {
    if (!workshop) {
      return '';
    }

    if (workshop.status === 'FULL') {
      return 'Workshop đã hết chỗ.';
    }

    if (workshop.status === 'CANCELLED') {
      return 'Workshop đã bị hủy.';
    }

    if (myRegistration) {
      return 'Bạn đã đăng ký workshop này.';
    }

    const registrationStart = dayjs(workshop.registrationStartTime);
    const registrationEnd = dayjs(workshop.registrationEndTime);
    const now = dayjs();

    if (registrationStart.isValid() && now.isBefore(registrationStart)) {
      return `Workshop mở đăng ký từ ${formatDateTime(workshop.registrationStartTime)}.`;
    }

    if (registrationEnd.isValid() && now.isAfter(registrationEnd)) {
      return 'Workshop đã hết hạn đăng ký.';
    }

    return '';
  }, [myRegistration, workshop]);

  const handleRegister = async () => {
    if (!workshop || unavailableReason) {
      setError(unavailableReason);
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      const registration = await registerWorkshop(workshop.id);
      const registrationId = registration.registrationId || registration.id;

      if (registration.status === 'PENDING') {
        setPendingRegistration(registration);
        setMyRegistration({
          id: registrationId,
          workshop,
          workshopId: workshop.id,
          status: registration.status,
          paymentStatus: 'PENDING',
        });
        return;
      }

      setSuccessRegistration(registration);
      setMyRegistration({
        id: registrationId,
        workshop,
        workshopId: workshop.id,
        status: registration.status,
        paymentStatus: 'FREE',
      });
    } catch (registerError) {
      setError(registerError.message);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <LoadingState rows={10} />;
  }

  if (error && !workshop) {
    return (
      <ErrorState
        title="Không tải được workshop"
        message={error}
        actionTo="/student/workshops"
        actionText="Quay lại danh sách"
      />
    );
  }

  return (
    <div className="page-stack">
      <Link to="/student/workshops">
        <Button icon={<ArrowLeftOutlined />}>Quay lại workshop</Button>
      </Link>

      {error && <Alert type="error" showIcon message={error} closable onClose={() => setError('')} />}

      <Card className="detail-hero-card" bordered={false}>
        <Space direction="vertical" size="middle">
          <Space wrap>
            <StatusBadge status={workshop.status} />
            <StatusBadge status={workshop.isPaid ? 'PAID_EVENT' : 'FREE'} type="price" />
            {myRegistration && (
              <StatusBadge status={pendingPayment ? 'PAID_PENDING' : 'REGISTERED'} />
            )}
          </Space>
          <Typography.Title>{workshop.title}</Typography.Title>
          <Typography.Paragraph>{workshop.description}</Typography.Paragraph>
          <Space wrap size="large">
            <Typography.Text>
              <UserOutlined /> {workshop.speakerName} · {workshop.speakerTitle}
            </Typography.Text>
            <Typography.Text>
              <CalendarOutlined /> {formatDate(workshop.date)} · {workshop.startTime}-{workshop.endTime}
            </Typography.Text>
            <Typography.Text>
              <EnvironmentOutlined /> {workshop.room}
            </Typography.Text>
            <Typography.Text>
              <ClockCircleOutlined /> Đăng ký: {formatDateTime(workshop.registrationStartTime)} -{' '}
              {formatDateTime(workshop.registrationEndTime)}
            </Typography.Text>
          </Space>
        </Space>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={16}>
          <Card title="Thông tin workshop" bordered={false}>
            <Descriptions column={1}>
              <Descriptions.Item label="Chủ đề">{workshop.topic}</Descriptions.Item>
              <Descriptions.Item label="Diễn giả">
                {workshop.speakerName} - {workshop.speakerTitle}
              </Descriptions.Item>
              <Descriptions.Item label="Giá">{formatMoney(workshop.price)}</Descriptions.Item>
              <Descriptions.Item label="Thời gian đăng ký">
                {formatDateTime(workshop.registrationStartTime)} - {formatDateTime(workshop.registrationEndTime)}
              </Descriptions.Item>
              <Descriptions.Item label="Sơ đồ phòng">
                {workshop.roomMapText || workshop.roomMapUrl}
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <Card title="Tóm tắt workshop" bordered={false} className="section-card">
            <Typography.Paragraph>
              <ReadOutlined /> {workshop.aiSummary}
            </Typography.Paragraph>
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Card className="registration-card" bordered={false}>
            <Space direction="vertical" size="large" className="full-width">
              <Row gutter={[12, 12]}>
                <Col span={12}>
                  <Statistic title="Sức chứa" value={workshop.capacity} />
                </Col>
                <Col span={12}>
                  <Statistic title="Đã đăng ký" value={workshop.registeredCount} />
                </Col>
                <Col span={24}>
                  <Statistic
                    title="Còn lại"
                    value={remainingSeats}
                    suffix="chỗ"
                    prefix={<TeamOutlined />}
                  />
                </Col>
              </Row>

              {myRegistration ? (
                <Link to={`/student/tickets/${myRegistration.id || myRegistration.registrationId}`}>
                  <Button
                    type="primary"
                    size="large"
                    block
                    style={pendingPayment ? { backgroundColor: '#faad14', borderColor: '#faad14' } : undefined}
                  >
                    {pendingPayment ? 'Hoàn tất thanh toán' : 'Xem QR ticket'}
                  </Button>
                </Link>
              ) : (
                <Button
                  type="primary"
                  size="large"
                  block
                  loading={submitting}
                  disabled={Boolean(unavailableReason)}
                  onClick={handleRegister}
                >
                  {workshop.isPaid ? 'Giữ chỗ và thanh toán sau' : 'Đăng ký'}
                </Button>
              )}

              {unavailableReason && (
                <Alert
                  type={myRegistration && !pendingPayment ? 'success' : 'warning'}
                  showIcon
                  message={unavailableReason}
                  description={
                    myRegistration
                      ? pendingPayment
                        ? 'Chỗ của bạn đã được giữ. Mở trang vé để tiếp tục thanh toán.'
                        : 'Bạn có thể mở QR ticket từ nút bên trên.'
                      : undefined
                  }
                />
              )}
            </Space>
          </Card>
        </Col>
      </Row>

      <Modal
        title="Đăng ký thành công"
        open={Boolean(successRegistration)}
        onCancel={() => setSuccessRegistration(null)}
        footer={[
          <Button
            key="ticket"
            type="primary"
            onClick={() => navigate(`/student/tickets/${successRegistration?.registrationId || successRegistration?.id}`)}
          >
            Xem ticket
          </Button>,
        ]}
      >
        <Result
          status="success"
          title="Bạn đã đăng ký workshop miễn phí thành công."
          subTitle="QR ticket đã sẵn sàng để xuất trình tại phòng."
        />
      </Modal>

      <Modal
        title="Đã giữ chỗ chờ thanh toán"
        open={Boolean(pendingRegistration)}
        onCancel={() => setPendingRegistration(null)}
        footer={[
          <Button key="close" onClick={() => setPendingRegistration(null)}>
            Ở lại trang này
          </Button>,
          pendingRegistration?.registrationId && (
            <Button key="ticket" onClick={() => navigate(`/student/tickets/${pendingRegistration.registrationId}`)}>
              Xem vé tạm
            </Button>
          ),
          <Button
            key="history"
            type="primary"
            onClick={() =>
              navigate('/student/my-registrations', {
                state: {
                  registrationAccepted: true,
                  registrationId: pendingRegistration?.registrationId,
                  message: pendingRegistration?.message,
                },
              })
            }
          >
            Mở đăng ký của tôi
          </Button>,
        ]}
      >
        <Result
          status="info"
          title="Chỗ của bạn đang được giữ trong hệ thống."
          subTitle={pendingRegistration?.message || 'Vào Đăng ký của tôi để hoàn tất thanh toán.'}
        />
      </Modal>
    </div>
  );
}
