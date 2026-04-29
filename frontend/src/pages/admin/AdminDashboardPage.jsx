import {
  CalendarOutlined,
  CheckCircleOutlined,
  FireOutlined,
  TeamOutlined,
  TrophyOutlined,
} from '@ant-design/icons';
import { Card, Col, List, Progress, Row, Skeleton, Table, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import AdminStatCard from '../../components/admin/AdminStatCard.jsx';
import PageHeader from '../../components/common/PageHeader.jsx';
import StatusBadge from '../../components/common/StatusBadge.jsx';
import { getAllRegistrations } from '../../services/registrationService.js';
import { getAllWorkshops } from '../../services/workshopService.js';
import { formatDate } from '../../utils/formatters.js';

export default function AdminDashboardPage() {
  const [workshops, setWorkshops] = useState([]);
  const [registrations, setRegistrations] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let ignore = false;

    async function loadDashboard() {
      setLoading(true);
      const [workshopResult, registrationResult] = await Promise.all([
        getAllWorkshops(),
        getAllRegistrations(),
      ]);

      if (!ignore) {
        setWorkshops(workshopResult);
        setRegistrations(registrationResult);
        setLoading(false);
      }
    }

    loadDashboard().catch(() => {
      if (!ignore) {
        setLoading(false);
      }
    });

    return () => {
      ignore = true;
    };
  }, []);

  const upcomingWorkshops = useMemo(
    () =>
      [...workshops]
        .filter((workshop) => workshop.status !== 'CANCELLED')
        .sort((first, second) =>
          `${first.date} ${first.startTime}`.localeCompare(`${second.date} ${second.startTime}`),
        )
        .slice(0, 5),
    [workshops],
  );

  const topWorkshops = useMemo(
    () =>
      [...workshops]
        .sort((first, second) => second.registeredCount - first.registeredCount)
        .slice(0, 4),
    [workshops],
  );

  const paidOrFreeRegistrations = useMemo(
    () =>
      registrations.filter((item) => ['PAID', 'FREE'].includes(item.paymentStatus)).length,
    [registrations],
  );

  const columns = [
    { title: 'Workshop', dataIndex: 'title', key: 'title' },
    {
      title: 'Ngày/giờ',
      key: 'dateTime',
      render: (_, record) => `${formatDate(record.date)} ${record.startTime}-${record.endTime}`,
    },
    { title: 'Phòng', dataIndex: 'room', key: 'room' },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status) => <StatusBadge status={status} />,
    },
  ];

  if (loading) {
    return <Skeleton active paragraph={{ rows: 10 }} />;
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="Bảng điều khiển"
        eyebrow="Organizer Portal"
        description="Tổng quan vận hành tuần lễ workshop UniHub."
      />

      <Row gutter={[16, 16]}>
        <Col xs={24} md={12} xl={5}>
          <AdminStatCard title="Tổng workshop" value={workshops.length} icon={<CalendarOutlined />} />
        </Col>
        <Col xs={24} md={12} xl={5}>
          <AdminStatCard
            title="Đang mở"
            value={workshops.filter((item) => item.status === 'OPEN').length}
            icon={<FireOutlined />}
            tone="green"
          />
        </Col>
        <Col xs={24} md={12} xl={5}>
          <AdminStatCard title="Lượt đăng ký" value={registrations.length} icon={<TeamOutlined />} />
        </Col>
        <Col xs={24} md={12} xl={5}>
          <AdminStatCard
            title="Đã xác nhận"
            value={paidOrFreeRegistrations}
            icon={<CheckCircleOutlined />}
            tone="green"
          />
        </Col>
        <Col xs={24} md={12} xl={4}>
          <AdminStatCard
            title="Đã full"
            value={workshops.filter((item) => item.status === 'FULL').length}
            icon={<TrophyOutlined />}
            tone="orange"
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={10}>
          <Card title="Workshop nổi bật theo đăng ký" bordered={false}>
            <List
              dataSource={topWorkshops}
              renderItem={(workshop) => (
                <List.Item>
                  <div className="admin-progress-row">
                    <Typography.Text strong>{workshop.title}</Typography.Text>
                    <Progress
                      percent={Math.round((workshop.registeredCount / workshop.capacity) * 100)}
                      size="small"
                    />
                    <Typography.Text type="secondary">
                      {workshop.registeredCount}/{workshop.capacity}
                    </Typography.Text>
                  </div>
                </List.Item>
              )}
            />
          </Card>
        </Col>
        <Col xs={24} lg={14}>
          <Card title="Workshop gần diễn ra" bordered={false}>
            <Table rowKey="id" columns={columns} dataSource={upcomingWorkshops} pagination={false} />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
