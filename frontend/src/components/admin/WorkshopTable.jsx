import { EyeOutlined, StopOutlined } from '@ant-design/icons';
import { Button, Popconfirm, Space, Table, Tag, Typography } from 'antd';
import { Link } from 'react-router-dom';
import StatusBadge from '../common/StatusBadge.jsx';
import { formatDate, formatDateTime, formatMoney } from '../../utils/formatters.js';

export default function WorkshopTable({ workshops, loading, onView, onCancel }) {
  const columns = [
    {
      title: 'Workshop',
      dataIndex: 'title',
      key: 'title',
      width: 260,
      render: (title, record) => (
        <Space direction="vertical" size={2}>
          <Typography.Text strong>{title}</Typography.Text>
          <Typography.Text type="secondary">{record.topic || 'Chưa phân loại'}</Typography.Text>
        </Space>
      ),
    },
    {
      title: 'Ngày/giờ',
      key: 'dateTime',
      render: (_, record) => `${formatDate(record.date)} ${record.startTime}-${record.endTime}`,
    },
    { title: 'Phòng', dataIndex: 'room', key: 'room' },
    { title: 'Sức chứa', dataIndex: 'capacity', key: 'capacity' },
    { title: 'Đã đăng ký', dataIndex: 'registeredCount', key: 'registeredCount' },
    {
      title: 'Còn lại',
      key: 'remaining',
      render: (_, record) => Math.max(record.capacity - record.registeredCount, 0),
    },
    {
      title: 'Mở đăng ký',
      key: 'registrationWindow',
      width: 240,
      render: (_, record) => (
        <Space direction="vertical" size={2}>
          <Typography.Text>{formatDateTime(record.registrationStartTime)}</Typography.Text>
          <Typography.Text type="secondary">đến {formatDateTime(record.registrationEndTime)}</Typography.Text>
        </Space>
      ),
    },
    {
      title: 'Giá',
      dataIndex: 'price',
      key: 'price',
      render: (price, record) => (
        <Tag color={record.isPaid ? 'gold' : 'green'}>{formatMoney(price)}</Tag>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status) => <StatusBadge status={status} />,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button icon={<EyeOutlined />} onClick={() => onView(record)} />
          <Link to={`/admin/workshops/${record.id}/edit`}>
            <Button>Sửa</Button>
          </Link>
          <Popconfirm
            title="Hủy workshop?"
            description="Workshop sẽ chuyển sang trạng thái đã hủy."
            okText="Hủy workshop"
            cancelText="Đóng"
            onConfirm={() => onCancel(record.id)}
            disabled={record.status === 'CANCELLED'}
          >
            <Button danger icon={<StopOutlined />} disabled={record.status === 'CANCELLED'} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={workshops}
      loading={loading}
      scroll={{ x: 1400 }}
      pagination={{ pageSize: 8 }}
    />
  );
}
