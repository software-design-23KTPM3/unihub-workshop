import {
  ArrowLeftOutlined,
  CalendarOutlined,
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
import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import ErrorState from '../../components/common/ErrorState.jsx';
import LoadingState from '../../components/common/LoadingState.jsx';
import StatusBadge from '../../components/common/StatusBadge.jsx';
import {
  completeMockPayment,
  getMyRegistrations,
  registerWorkshop,
} from '../../services/registrationService.js';
import { getWorkshopById } from '../../services/workshopService.js';
import { formatDate, formatMoney } from '../../utils/formatters.js';

export default function StudentWorkshopDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [workshop, setWorkshop] = useState(null);
  const [myRegistration, setMyRegistration] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [successRegistration, setSuccessRegistration] = useState(null);
  const [paymentRegistration, setPaymentRegistration] = useState(null);
  const [paymentStatus, setPaymentStatus] = useState('idle');

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
                registration.workshopId === id && registration.status !== 'CANCELLED',
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

      if (workshop.isPaid) {
        setPaymentRegistration(registration);
        setPaymentStatus('pending');
        return;
      }

      setSuccessRegistration(registration);
      setMyRegistration(registration);
    } catch (registerError) {
      setError(registerError.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handlePaymentSuccess = async () => {
    setPaymentStatus('success');
    const paidRegistration = await completeMockPayment(paymentRegistration.registrationId);
    setMyRegistration(paidRegistration);
    setPaymentRegistration(null);
    navigate(`/student/tickets/${paidRegistration.id}`);
  };

  const handlePaymentTimeout = () => {
    setPaymentStatus('timeout');
    setError('Thanh toán quá thời gian. Đăng ký đang ở trạng thái chờ thanh toán.');
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
              <Descriptions.Item label="Sơ đồ phòng">
                {workshop.roomMapText || workshop.roomMapUrl}
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <Card title="AI Summary" bordered={false} className="section-card">
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
                <Link to={`/student/tickets/${myRegistration.id}`}>
                  <Button type="primary" size="large" block>
                    Xem QR ticket
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
                  Đăng ký
                </Button>
              )}

              {unavailableReason && (
                <Alert type="warning" showIcon message={unavailableReason} />
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
            onClick={() => navigate(`/student/tickets/${successRegistration?.registrationId}`)}
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
        title="Thanh toán giả lập"
        open={Boolean(paymentRegistration)}
        onCancel={() => setPaymentRegistration(null)}
        footer={[
          <Button key="close" onClick={() => setPaymentRegistration(null)}>
            Đóng
          </Button>,
          <Button
            key="history"
            type="primary"
            onClick={() => navigate('/student/my-registrations')}
          >
            Xem Lịch sử đăng ký
          </Button>,
        ]}
      >
        <Space direction="vertical" size="middle" className="full-width">
          <Alert
            type="info"
            showIcon
            message="Yêu cầu của bạn đang được xử lý. Vui lòng vào mục 'Lịch sử đăng ký' để hoàn tất thanh toán trong vòng 30 phút để giữ chỗ chính thức."
          />
          <Typography.Paragraph>
            Lưu ý: Có thể mất một vài giây để đơn hàng xuất hiện trong lịch sử của bạn.
          </Typography.Paragraph>
        </Space>
      </Modal>
    </div>
  );
}
