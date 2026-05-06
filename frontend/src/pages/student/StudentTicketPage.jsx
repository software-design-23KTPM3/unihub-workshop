import { Alert, Button } from 'antd';
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import ErrorState from '../../components/common/ErrorState.jsx';
import LoadingState from '../../components/common/LoadingState.jsx';
import PageHeader from '../../components/common/PageHeader.jsx';
import TicketCard from '../../components/workshop/TicketCard.jsx';
import { useAuth } from '../../hooks/useAuth.js';
import { getRegistrationById, completeMockPayment } from '../../services/registrationService.js';

export default function StudentTicketPage() {
  const { registrationId } = useParams();
  const { currentUser } = useAuth();
  const [registration, setRegistration] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [paying, setPaying] = useState(false);

  const handlePaymentSuccess = async () => {
    setPaying(true);
    try {
      const updated = await completeMockPayment(registrationId);
      setRegistration(updated);
    } catch (err) {
      setError('Thanh toán thất bại: ' + err.message);
    } finally {
      setPaying(false);
    }
  };

  useEffect(() => {
    let ignore = false;

    async function loadTicket() {
      setLoading(true);
      setError('');

      let retries = 0;
      const maxRetries = 5;

      const attemptLoad = async () => {
        try {
          const result = await getRegistrationById(registrationId);
          if (!ignore) {
            setRegistration(result);
            setLoading(false);
          }
        } catch (loadError) {
          if (retries < maxRetries && loadError.status === 404) {
            retries++;
            setTimeout(attemptLoad, 1000); // Retry after 1s
          } else if (!ignore) {
            setError(loadError.message);
            setLoading(false);
          }
        }
      };

      attemptLoad();
    }

    loadTicket();

    return () => {
      ignore = true;
    };
  }, [registrationId]);

  if (loading) {
    return <LoadingState rows={10} />;
  }

  if (error) {
    return (
      <ErrorState
        title="Không tìm thấy ticket"
        message={error}
        actionTo="/student/my-registrations"
        actionText="Về đăng ký của tôi"
      />
    );
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="Thông tin vé"
        description="Thông tin chi tiết về buổi workshop bạn đã đăng ký."
        extra={
          <Link to="/student/my-registrations">
            <Button>Đăng ký của tôi</Button>
          </Link>
        }
      />
      {registration.status === 'PENDING' ? (
        <Alert
          type="warning"
          showIcon
          message="Vé đang chờ thanh toán. Sau khi thanh toán, mã QR sẽ được gửi về email của bạn."
          action={
            <Button 
              type="primary" 
              loading={paying} 
              onClick={handlePaymentSuccess}
              style={{ backgroundColor: '#faad14', borderColor: '#faad14' }}
            >
              Thanh toán ngay (Demo)
            </Button>
          }
        />
      ) : (
        <Alert
          type="success"
          showIcon
          message="Vé hợp lệ. Mã QR đã được gửi về email của bạn."
        />
      )}
      <TicketCard registration={registration} currentUser={currentUser} />
    </div>
  );
}
