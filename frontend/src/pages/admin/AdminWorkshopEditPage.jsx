import { Result, Skeleton, message } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import WorkshopForm, { toWorkshopFormValues } from '../../components/admin/WorkshopForm.jsx';
import PageHeader from '../../components/common/PageHeader.jsx';
import { getWorkshopById, updateWorkshop } from '../../services/workshopService.js';

export default function AdminWorkshopEditPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [workshop, setWorkshop] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    let ignore = false;

    async function loadWorkshop() {
      setLoading(true);
      try {
        const result = await getWorkshopById(id);
        if (!ignore) {
          setWorkshop(result);
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

    loadWorkshop();

    return () => {
      ignore = true;
    };
  }, [id]);

  const handleSubmit = async (payload) => {
    setSubmitting(true);
    try {
      await updateWorkshop(id, payload);
      messageApi.success('Cập nhật workshop thành công.');
      navigate('/admin/workshops');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <Skeleton active paragraph={{ rows: 10 }} />;
  }

  if (error) {
    return <Result status="warning" title="Không tải được workshop" subTitle={error} />;
  }

  return (
    <div className="page-stack">
      {contextHolder}
      <PageHeader
        title="Chỉnh sửa workshop"
        eyebrow="Organizer Portal"
        description="Cập nhật lịch, phòng, sức chứa và nội dung workshop."
      />
      <WorkshopForm
        key={workshop.id}
        initialValues={toWorkshopFormValues(workshop)}
        submitting={submitting}
        submitText="Lưu thay đổi"
        onSubmit={handleSubmit}
      />
    </div>
  );
}
