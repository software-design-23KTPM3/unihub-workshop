import { message } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import WorkshopForm, { toWorkshopFormValues } from '../../components/admin/WorkshopForm.jsx';
import PageHeader from '../../components/common/PageHeader.jsx';
import { createWorkshop, uploadWorkshopPdf } from '../../services/workshopService.js';

export default function AdminWorkshopCreatePage() {
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();

  const handleSubmit = async (payload, pdfFile) => {
    setSubmitting(true);
    try {
      const createdWorkshop = await createWorkshop(payload);
      if (pdfFile && createdWorkshop?.id) {
        await uploadWorkshopPdf(createdWorkshop.id, pdfFile);
      }
      messageApi.success('Tạo workshop thành công.');
      navigate('/admin/workshops');
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
