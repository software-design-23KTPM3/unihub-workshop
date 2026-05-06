import { Card, Form, Select } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import RegistrationTable from '../../components/admin/RegistrationTable.jsx';
import PageHeader from '../../components/common/PageHeader.jsx';
import { getAllRegistrations } from '../../services/registrationService.js';
import { getAllWorkshops } from '../../services/workshopService.js';

const statusOptions = [
  { value: 'PENDING', label: 'PENDING' },
  { value: 'SUCCESS', label: 'SUCCESS' },
  { value: 'FAILED', label: 'FAILED' },
  { value: 'CHECKED_IN', label: 'CHECKED_IN' },
];

export default function AdminRegistrationsPage() {
  const [registrations, setRegistrations] = useState([]);
  const [workshops, setWorkshops] = useState([]);
  const [filters, setFilters] = useState({});
  const [loading, setLoading] = useState(true);

  const workshopOptions = useMemo(
    () => workshops.map((workshop) => ({ value: workshop.id, label: workshop.title })),
    [workshops],
  );

  const loadRegistrations = async (nextFilters = filters) => {
    setLoading(true);
    const [registrationResult, workshopResult] = await Promise.all([
      getAllRegistrations(nextFilters),
      getAllWorkshops(),
    ]);
    setRegistrations(registrationResult);
    setWorkshops(workshopResult);
    setLoading(false);
  };

  useEffect(() => {
    loadRegistrations();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleFilterChange = (_, values) => {
    const nextFilters = {
      workshopId: values.workshopId,
      status: values.status,
    };
    setFilters(nextFilters);
    loadRegistrations(nextFilters);
  };

  return (
    <div className="page-stack">
      <PageHeader
        title="Danh sách đăng ký"
        eyebrow="Organizer Portal"
        description="Theo dõi sinh viên, trạng thái thanh toán và QR code."
      />

      <Card bordered={false}>
        <Form layout="vertical" onValuesChange={handleFilterChange}>
          <div className="admin-filter-grid admin-filter-grid--compact">
            <Form.Item label="Workshop" name="workshopId">
              <Select showSearch allowClear options={workshopOptions} optionFilterProp="label" />
            </Form.Item>
            <Form.Item label="Trạng thái" name="status">
              <Select allowClear options={statusOptions} />
            </Form.Item>
          </div>
        </Form>
      </Card>

      <Card bordered={false}>
        <RegistrationTable registrations={registrations} loading={loading} />
      </Card>
    </div>
  );
}
