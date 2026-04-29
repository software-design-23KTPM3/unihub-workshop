import { CalendarOutlined, EnvironmentOutlined, QrcodeOutlined } from '@ant-design/icons';
import { Button, Card, Col, Row, Space, Statistic, Tag, Typography } from 'antd';
import dayjs from 'dayjs';
import { QRCodeCanvas } from 'qrcode.react';
import PageHeader from '../../components/common/PageHeader.jsx';

export default function StudentHomePage() {
  return (
    <div className="page-stack">
      <section className="student-hero">
        <div>
          <Typography.Title>UniHub Career Workshop Week</Typography.Title>
          <Typography.Paragraph>
            Cổng thông tin workshop nghề nghiệp, kỹ năng và công nghệ dành cho sinh viên.
          </Typography.Paragraph>
          <Space wrap>
            <Button type="primary" size="large" icon={<CalendarOutlined />}>
              Khám phá workshop
            </Button>
            <Button size="large" icon={<QrcodeOutlined />}>
              Vé QR của tôi
            </Button>
          </Space>
        </div>
      </section>

      <PageHeader
        title="Trang sinh vien"
        description="Placeholder ban đầu cho danh sách workshop, chi tiết, đăng ký và ticket QR."
      />

      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="Workshop đang mở" value={12} suffix="sự kiện" />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="Vé đã đăng ký" value={2} suffix="vé" />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="Ngay su kien" value={5} suffix="ngay" />
          </Card>
        </Col>
      </Row>

      <Card title="Workshop noi bat">
        <Space className="featured-workshop" size="large" align="start">
          <Space direction="vertical" size="small">
            <Typography.Text strong>AI in Career Development</Typography.Text>
            <Typography.Text>
              <EnvironmentOutlined /> Hall A - {dayjs().add(2, 'day').format('DD/MM/YYYY HH:mm')}
            </Typography.Text>
            <Space>
              <Tag color="blue">Còn 36 chỗ</Tag>
              <Tag color="green">Miễn phí</Tag>
            </Space>
          </Space>
          <div className="ticket-preview">
            <QRCodeCanvas value="UNIHUB-DEMO-TICKET-2312345" size={88} />
          </div>
        </Space>
      </Card>
    </div>
  );
}
