import { Alert, Button } from 'antd';
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import ErrorState from '../../components/common/ErrorState.jsx';
import LoadingState from '../../components/common/LoadingState.jsx';
import PageHeader from '../../components/common/PageHeader.jsx';
import TicketCard from '../../components/workshop/TicketCard.jsx';
import { useAuth } from '../../hooks/useAuth.js';
import { getRegistrationById } from '../../services/registrationService.js';

export default function StudentTicketPage() {
  const { registrationId } = useParams();
  const { currentUser } = useAuth();
  const [registration, setRegistration] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let ignore = false;

    async function loadTicket() {
      setLoading(true);
      setError('');

      try {
        const result = await getRegistrationById(registrationId);

        if (!ignore) {
          setRegistration(result);
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
        title="QR Ticket"
        description="Xuất trình vé này tại quầy xác nhận của workshop."
        extra={
          <Link to="/student/my-registrations">
            <Button>Đăng ký của tôi</Button>
          </Link>
        }
      />
      {registration.status === 'PAID_PENDING' && (
        <Alert
          type="warning"
          showIcon
          message="Vé đang chờ thanh toán. QR chỉ hợp lệ sau khi thanh toán thành công."
        />
      )}
      <TicketCard registration={registration} currentUser={currentUser} />
    </div>
  );
}
