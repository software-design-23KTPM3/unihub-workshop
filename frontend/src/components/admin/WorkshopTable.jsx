import { EyeOutlined, StopOutlined } from '@ant-design/icons';
import { Button, Popconfirm, Space, Table } from 'antd';
import { Link } from 'react-router-dom';
import StatusBadge from '../common/StatusBadge.jsx';
import { formatDate, formatMoney } from '../../utils/formatters.js';

export default function WorkshopTable({ workshops, loading, onView, onCancel }) {
  const columns = [
    {
      title: 'Workshop',
      dataIndex: 'title',
      key: 'title',
      width: 260,
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
      title: 'Giá',
      dataIndex: 'price',
      key: 'price',
      render: (price) => formatMoney(price),
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
          <Button icon={<EyeOutlined />} onClick={() => onView(record)}>
            Xem
          </Button>
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
            <Button danger icon={<StopOutlined />} disabled={record.status === 'CANCELLED'}>
              Hủy
            </Button>
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
      scroll={{ x: 1200 }}
      pagination={{ pageSize: 8 }}
    />
  );
}
