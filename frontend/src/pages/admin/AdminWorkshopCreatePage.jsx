import { message } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import WorkshopForm, { toWorkshopFormValues } from '../../components/admin/WorkshopForm.jsx';
import PageHeader from '../../components/common/PageHeader.jsx';
import { createWorkshop } from '../../services/workshopService.js';

export default function AdminWorkshopCreatePage() {
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();

  const handleSubmit = async (payload) => {
    setSubmitting(true);
    try {
      await createWorkshop(payload);
      messageApi.success('Tạo workshop thành công.');
      navigate('/admin/workshops');
    } catch (error) {
      messageApi.error(error.message || 'Không tạo được workshop.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page-stack">
      {contextHolder}
      <PageHeader
        title="Tạo workshop"
        eyebrow="Organizer Portal"
        description="Nhập thông tin phiên workshop và mô phỏng AI summary từ PDF."
      />
      <WorkshopForm
        initialValues={toWorkshopFormValues(null)}
        submitting={submitting}
        submitText="Tạo workshop"
        onSubmit={handleSubmit}
      />
    </div>
  );
}
