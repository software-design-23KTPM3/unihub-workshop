import { Button, Card, DatePicker, Form, Input, Modal, Select, Space, message } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import WorkshopTable from '../../components/admin/WorkshopTable.jsx';
import PageHeader from '../../components/common/PageHeader.jsx';
import StatusBadge from '../../components/common/StatusBadge.jsx';
import { cancelWorkshop, getAllWorkshops } from '../../services/workshopService.js';
import { formatDate, formatMoney } from '../../utils/formatters.js';

const statusOptions = [
  { value: 'OPEN', label: 'OPEN' },
  { value: 'FULL', label: 'FULL' },
  { value: 'CANCELLED', label: 'CANCELLED' },
];

export default function AdminWorkshopsPage() {
  const [workshops, setWorkshops] = useState([]);
  const [allWorkshops, setAllWorkshops] = useState([]);
  const [filters, setFilters] = useState({});
  const [loading, setLoading] = useState(true);
  const [selectedWorkshop, setSelectedWorkshop] = useState(null);
  const [messageApi, contextHolder] = message.useMessage();

  const topics = useMemo(
    () => [...new Set(allWorkshops.map((workshop) => workshop.topic))].sort(),
    [allWorkshops],
  );

  const loadWorkshops = async (nextFilters = filters) => {
    setLoading(true);
    const result = await getAllWorkshops(nextFilters);
    const allResult = await getAllWorkshops();
    setWorkshops(result);
    setAllWorkshops(allResult);
    setLoading(false);
  };

  useEffect(() => {
    loadWorkshops();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleFilterChange = (_, values) => {
    const nextFilters = {
      keyword: values.keyword,
      date: values.date?.format('YYYY-MM-DD'),
      topic: values.topic,
      status: values.status,
    };
    setFilters(nextFilters);
    loadWorkshops(nextFilters);
  };

  const handleCancel = async (id) => {
    await cancelWorkshop(id);
    messageApi.success('Đã hủy workshop.');
    loadWorkshops();
  };

  return (
    <div className="page-stack">
      {contextHolder}
      <PageHeader
        title="Quản lý workshop"
        eyebrow="Organizer Portal"
        description="Theo dõi lịch, sức chứa, trạng thái và thao tác quản trị workshop."
        extra={
          <Link to="/admin/workshops/create">
            <Button type="primary">Tạo workshop</Button>
          </Link>
        }
      />

      <Card bordered={false}>
        <Form layout="vertical" onValuesChange={handleFilterChange}>
          <div className="admin-filter-grid">
            <Form.Item label="Tìm kiếm" name="keyword">
              <Input.Search allowClear placeholder="Title, speaker, tag..." />
            </Form.Item>
            <Form.Item label="Trạng thái" name="status">
              <Select allowClear options={statusOptions} />
            </Form.Item>
            <Form.Item label="Ngày" name="date">
              <DatePicker className="full-width" format="DD/MM/YYYY" />
            </Form.Item>
            <Form.Item label="Chủ đề" name="topic">
              <Select allowClear options={topics.map((topic) => ({ value: topic, label: topic }))} />
            </Form.Item>
          </div>
        </Form>
      </Card>

      <Card bordered={false}>
        <WorkshopTable
          workshops={workshops}
          loading={loading}
          onView={setSelectedWorkshop}
          onCancel={handleCancel}
        />
      </Card>

      <Modal
        title="Chi tiết workshop"
        open={Boolean(selectedWorkshop)}
        onCancel={() => setSelectedWorkshop(null)}
        footer={null}
      >
        {selectedWorkshop && (
          <Space direction="vertical" size="middle" className="full-width">
            <StatusBadge status={selectedWorkshop.status} />
            <h3>{selectedWorkshop.title}</h3>
            <p>{selectedWorkshop.description}</p>
            <p>
              <strong>Diễn giả:</strong> {selectedWorkshop.speakerName} -{' '}
              {selectedWorkshop.speakerTitle}
            </p>
            <p>
              <strong>Thời gian:</strong> {formatDate(selectedWorkshop.date)}{' '}
              {selectedWorkshop.startTime}-{selectedWorkshop.endTime}
            </p>
            <p>
              <strong>Phòng:</strong> {selectedWorkshop.room}
            </p>
            <p>
              <strong>Giá:</strong> {formatMoney(selectedWorkshop.price)}
            </p>
            <p>
              <strong>AI Summary:</strong> {selectedWorkshop.aiSummary}
            </p>
          </Space>
        )}
      </Modal>
    </div>
  );
}
