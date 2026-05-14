import { Alert, Button, QRCode, Space, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import ErrorState from '../../components/common/ErrorState.jsx';
import LoadingState from '../../components/common/LoadingState.jsx';
import PageHeader from '../../components/common/PageHeader.jsx';
import { getRegistrationById, startRegistrationPayment } from '../../services/registrationService.js';
import { formatMoney } from '../../utils/formatters.js';

function readStoredPaymentSession(registrationId) {
  try {
    return JSON.parse(sessionStorage.getItem(`payment-session:${registrationId}`) || 'null');
  } catch {
    return null;
  }
}

function sleep(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

function isAsyncRegistrationPending(error) {
  const message = String(error?.message || '').toLowerCase();
  return error?.status === 404 ||
    message.includes('registration not found') ||
    message.includes('payment transaction not found');
}

export default function StudentPaymentPage() {
  const { registrationId } = useParams();
  const [searchParams] = useSearchParams();
  const [registration, setRegistration] = useState(null);
  const [paymentSession, setPaymentSession] = useState(() => readStoredPaymentSession(registrationId));
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [starting, setStarting] = useState(false);

  const paymentUrl = paymentSession?.paymentUrl || '';
  const isCompleted = registration?.status === 'SUCCESS' || registration?.status === 'CHECKED_IN';
  const paymentResult = searchParams.get('paymentResult');

  const amountText = useMemo(() => (
    paymentSession?.amount ? formatMoney(paymentSession.amount) : ''
  ), [paymentSession?.amount]);

  const startPaymentWhenReady = async () => {
    let lastError;

    for (let attempt = 0; attempt < 8; attempt += 1) {
      try {
        const registrationResult = await getRegistrationById(registrationId);
        setRegistration(registrationResult);
        return await startRegistrationPayment(registrationId);
      } catch (err) {
        lastError = err;
        if (!isAsyncRegistrationPending(err)) {
          throw err;
        }
        await sleep(1000);
      }
    }

    throw lastError || new Error('Chưa thể mở thanh toán. Vui lòng thử lại sau.');
  };

  const retryPayment = async () => {
    sessionStorage.removeItem(`payment-session:${registrationId}`);
    setError('');
    setStarting(true);
    try {
      const payment = await startPaymentWhenReady();
      setPaymentSession(payment);
      sessionStorage.setItem(`payment-session:${registrationId}`, JSON.stringify(payment));
      window.history.replaceState(null, '', `/student/tickets/${registrationId}/payment`);
    } catch (err) {
      setPaymentSession(null);
      setError(err.message || 'Dịch vụ thanh toán đang tạm gián đoạn. Vui lòng thử lại sau.');
    } finally {
      setStarting(false);
    }
  };

  useEffect(() => {
    let ignore = false;
    let timer;

    async function loadRegistration() {
      try {
        const result = await getRegistrationById(registrationId);
        if (!ignore) {
          setRegistration(result);
          setLoading(false);
        }
      } catch (err) {
        if (!ignore) {
          if (isAsyncRegistrationPending(err)) {
            setLoading(false);
          } else {
            setError(err.message || 'Không tải được trạng thái thanh toán.');
            setLoading(false);
          }
        }
      }
    }

    loadRegistration();
    timer = setInterval(loadRegistration, 3000);

    return () => {
      ignore = true;
      clearInterval(timer);
    };
  }, [registrationId]);

  useEffect(() => {
    if (paymentSession?.paymentUrl || isCompleted || paymentResult === 'failed' || paymentResult === 'retry') {
      return;
    }

    let ignore = false;
    async function createPaymentSession() {
      setStarting(true);
      setError('');
      try {
        const payment = await startPaymentWhenReady();
        if (!ignore) {
          setPaymentSession(payment);
          sessionStorage.setItem(`payment-session:${registrationId}`, JSON.stringify(payment));
        }
      } catch (err) {
        if (!ignore) {
          setError(err.message || 'Dịch vụ thanh toán đang tạm gián đoạn.');
        }
      } finally {
        if (!ignore) {
          setStarting(false);
        }
      }
    }

    createPaymentSession();
    return () => {
      ignore = true;
    };
  }, [registrationId, paymentSession?.paymentUrl, isCompleted]);

  if (loading) {
    return <LoadingState rows={8} />;
  }

  if (error && !starting && (!paymentUrl || paymentResult === 'retry')) {
    return (
      <ErrorState
        title="Thanh toán tạm thời không khả dụng"
        message={error}
        actionTo={`/student/tickets/${registrationId}`}
        actionText="Quay lại vé"
      />
    );
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="Thanh toán workshop"
        description="Mở liên kết hoặc quét QR để hoàn tất thanh toán workshop."
        extra={
          <Link to={`/student/tickets/${registrationId}`}>
            <Button>Quay lại vé</Button>
          </Link>
        }
      />

      {isCompleted ? (
        <Alert
          type="success"
          showIcon
          message="Thanh toán thành công"
          description="Vé và QR check-in của bạn đã có hiệu lực."
          action={
            <Link to={`/student/tickets/${registrationId}`}>
              <Button type="primary">Xem vé</Button>
            </Link>
          }
        />
      ) : paymentResult === 'success' ? (
        <Alert
          type="info"
          showIcon
          message="Đang kiểm tra đăng ký"
          description="Thanh toán đã được xác nhận. Vui lòng chờ trong giây lát hoặc kiểm tra lại vé của bạn."
          action={
            <Link to={`/student/tickets/${registrationId}`}>
              <Button>Kiểm tra vé</Button>
            </Link>
          }
        />
      ) : paymentResult === 'failed' ? (
        <Alert
          type="warning"
          showIcon
          message="Thanh toán chưa thành công"
          description="Giao dịch chưa được hoàn tất. Bạn có thể thử lại bằng một phiên thanh toán mới."
          action={
            <Button type="primary" onClick={retryPayment} loading={starting}>
              Thử lại
            </Button>
          }
        />
      ) : paymentResult === 'retry' ? (
        <Alert
          type="error"
          showIcon
          message="Cổng thanh toán tạm thời gặp sự cố"
          description="Giao dịch chưa hoàn tất. Vui lòng thử lại sau ít phút."
          action={
            <Button type="primary" onClick={retryPayment} loading={starting}>
              Thử mở lại thanh toán
            </Button>
          }
        />
      ) : (
        <Alert
          type="info"
          showIcon
          message={starting ? 'Đang chuẩn bị phiên thanh toán' : 'Chờ sinh viên hoàn tất thanh toán'}
          description={
            starting
              ? 'Hệ thống đang xác nhận chỗ đã giữ và mở cổng thanh toán. Vui lòng chờ trong giây lát.'
              : 'Sau khi hoàn tất thanh toán, trang này sẽ tự cập nhật trạng thái vé.'
          }
        />
      )}

      {error && paymentUrl && (
        <Alert type="error" showIcon message="Không cập nhật được trạng thái" description={error} />
      )}

      {!isCompleted && paymentUrl && !paymentResult && (
        <section className="payment-qr-card">
          <div className="payment-qr-panel">
            <QRCode value={paymentUrl} size={196} bordered={false} />
            <Space direction="vertical" size={10} className="payment-qr-panel__content">
              <Typography.Text strong>Mã giao dịch: {paymentSession.gatewayPaymentId}</Typography.Text>
              {amountText && <Typography.Text>Số tiền: {amountText}</Typography.Text>}
              <Typography.Text>
                QR này chứa liên kết thanh toán. Nếu thiết bị khác không mở được localhost, hãy dùng liên kết bên dưới trên máy đang chạy hệ thống.
              </Typography.Text>
              <Typography.Link href={paymentUrl}>
                {paymentUrl}
              </Typography.Link>
              <Space wrap>
                <Button type="primary" href={paymentUrl}>
                  Mở cổng thanh toán
                </Button>
                <Button onClick={() => window.location.reload()} loading={starting}>
                  Làm mới
                </Button>
              </Space>
            </Space>
          </div>
        </section>
      )}
    </div>
  );
}
