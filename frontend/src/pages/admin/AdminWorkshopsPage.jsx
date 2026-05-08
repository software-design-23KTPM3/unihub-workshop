import { Button, Card, DatePicker, Descriptions, Form, Input, Modal, Select, Space, Tag, message } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import WorkshopTable from '../../components/admin/WorkshopTable.jsx';
import PageHeader from '../../components/common/PageHeader.jsx';
import StatusBadge from '../../components/common/StatusBadge.jsx';
import { cancelWorkshop, getAllWorkshops } from '../../services/workshopService.js';
import { formatDate, formatDateTime, formatMoney } from '../../utils/formatters.js';

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
    try {
      const result = await getAllWorkshops(nextFilters);
      const allResult = await getAllWorkshops();
      setWorkshops(result);
      setAllWorkshops(allResult);
    } catch (error) {
      messageApi.error(error.message || 'Không tải được danh sách workshop.');
    } finally {
      setLoading(false);
    }
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
    try {
      await cancelWorkshop(id);
      messageApi.success('Đã hủy workshop.');
      loadWorkshops();
    } catch (error) {
      messageApi.error(error.message || 'Không hủy được workshop.');
    }
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
        width={720}
      >
        {selectedWorkshop && (
          <Space direction="vertical" size="middle" className="full-width">
            <Space wrap>
              <StatusBadge status={selectedWorkshop.status} />
              <Tag color={selectedWorkshop.isPaid ? 'gold' : 'green'}>
                {formatMoney(selectedWorkshop.price)}
              </Tag>
              {(selectedWorkshop.tags || []).map((tag) => (
                <Tag key={tag}>{tag}</Tag>
              ))}
            </Space>
            <h2 className="admin-modal-title">{selectedWorkshop.title}</h2>
            <p>{selectedWorkshop.description}</p>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="Diễn giả">
                {selectedWorkshop.speakerName || 'Chưa cập nhật'} - {selectedWorkshop.speakerTitle || 'N/A'}
              </Descriptions.Item>
              <Descriptions.Item label="Thời gian diễn ra">
                {formatDate(selectedWorkshop.date)} {selectedWorkshop.startTime}-{selectedWorkshop.endTime}
              </Descriptions.Item>
              <Descriptions.Item label="Mở đăng ký">
                {formatDateTime(selectedWorkshop.registrationStartTime)} -{' '}
                {formatDateTime(selectedWorkshop.registrationEndTime)}
              </Descriptions.Item>
              <Descriptions.Item label="Phòng">{selectedWorkshop.room}</Descriptions.Item>
              <Descriptions.Item label="Sức chứa">
                {selectedWorkshop.registeredCount}/{selectedWorkshop.capacity} đã đăng ký
              </Descriptions.Item>
              <Descriptions.Item label="Sơ đồ phòng">
                {selectedWorkshop.roomMapText || 'Chưa cập nhật'}
              </Descriptions.Item>
              <Descriptions.Item label="Tóm tắt tự động">
                {selectedWorkshop.aiSummary || 'Chưa có tóm tắt'}
              </Descriptions.Item>
            </Descriptions>
          </Space>
        )}
      </Modal>
    </div>
  );
}
