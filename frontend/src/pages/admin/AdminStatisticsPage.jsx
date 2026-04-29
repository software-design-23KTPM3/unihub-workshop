import { Card, Col, Progress, Row, Table, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import AdminStatCard from '../../components/admin/AdminStatCard.jsx';
import PageHeader from '../../components/common/PageHeader.jsx';
import { getAllRegistrations } from '../../services/registrationService.js';
import { getAllWorkshops } from '../../services/workshopService.js';

export default function AdminStatisticsPage() {
  const [workshops, setWorkshops] = useState([]);
  const [registrations, setRegistrations] = useState([]);

  useEffect(() => {
    let ignore = false;

    async function loadStats() {
      const [workshopResult, registrationResult] = await Promise.all([
        getAllWorkshops(),
        getAllRegistrations(),
      ]);

      if (!ignore) {
        setWorkshops(workshopResult);
        setRegistrations(registrationResult);
      }
    }

    loadStats();

    return () => {
      ignore = true;
    };
  }, []);

  const registrationByWorkshop = useMemo(
    () =>
      workshops.map((workshop) => ({
        key: workshop.id,
        title: workshop.title,
        registrations: registrations.filter((item) => item.workshopId === workshop.id).length,
        capacity: workshop.capacity,
        registeredCount: workshop.registeredCount,
      })),
    [registrations, workshops],
  );

  const paidCount = registrations.filter((item) => item.paymentStatus === 'PAID').length;
  const freeCount = registrations.filter((item) => item.paymentStatus === 'FREE').length;
  const pendingCount = registrations.filter((item) => item.paymentStatus === 'PENDING').length;
  const confirmedRate = registrations.length
    ? Math.round(((paidCount + freeCount) / registrations.length) * 100)
    : 0;

  const columns = [
    { title: 'Workshop', dataIndex: 'title', key: 'title' },
    { title: 'Số lượt đăng ký', dataIndex: 'registrations', key: 'registrations' },
    {
      title: 'Tỷ lệ lấp đầy',
      key: 'fillRate',
      render: (_, record) => (
        <Progress
          percent={Math.round((record.registeredCount / record.capacity) * 100)}
          size="small"
        />
      ),
    },
  ];

  return (
    <div className="page-stack">
      <PageHeader
        title="Thống kê"
        eyebrow="Organizer Portal"
        description="Tổng hợp đăng ký, thanh toán và phân bổ workshop để báo cáo demo."
      />

      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <AdminStatCard title="Tỷ lệ xác nhận" value={confirmedRate} suffix="%" tone="green" />
        </Col>
        <Col xs={24} md={8}>
          <AdminStatCard title="Đăng ký đã thanh toán" value={paidCount} tone="green" />
        </Col>
        <Col xs={24} md={8}>
          <AdminStatCard title="Chờ thanh toán" value={pendingCount} tone="orange" />
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={10}>
          <Card title="Phân bổ miễn phí/có phí" bordered={false}>
            <Typography.Text>Đã thanh toán</Typography.Text>
            <Progress percent={registrations.length ? Math.round((paidCount / registrations.length) * 100) : 0} />
            <Typography.Text>Miễn phí</Typography.Text>
            <Progress percent={registrations.length ? Math.round((freeCount / registrations.length) * 100) : 0} status="success" />
            <Typography.Text>Đang chờ</Typography.Text>
            <Progress percent={registrations.length ? Math.round((pendingCount / registrations.length) * 100) : 0} status="exception" />
          </Card>
        </Col>
        <Col xs={24} lg={14}>
          <Card title="Số đăng ký theo workshop" bordered={false}>
            <Table
              rowKey="key"
              columns={columns}
              dataSource={registrationByWorkshop}
              pagination={{ pageSize: 6 }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
