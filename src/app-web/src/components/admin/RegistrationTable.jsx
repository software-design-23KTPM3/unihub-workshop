import { QrcodeOutlined } from '@ant-design/icons';
import { Space, Table, Typography } from 'antd';
import StatusBadge from '../common/StatusBadge.jsx';
import { formatDateTime } from '../../utils/formatters.js';

export default function RegistrationTable({ registrations, loading }) {
  const columns = [
    { title: 'Sinh viên', dataIndex: 'studentName', key: 'studentName' },
    { title: 'Email', dataIndex: 'studentEmail', key: 'studentEmail' },
    {
      title: 'Workshop',
      key: 'workshop',
      render: (_, record) => record.workshop?.title || record.workshopId,
    },
    {
      title: 'Thời gian đăng ký',
      dataIndex: 'registeredAt',
      key: 'registeredAt',
      render: (value) => formatDateTime(value),
    },
    {
      title: 'Thanh toán',
      dataIndex: 'paymentStatus',
      key: 'paymentStatus',
      render: (status) => <StatusBadge status={status} type="payment" />,
    },
    {
      title: 'Đăng ký',
      dataIndex: 'status',
      key: 'status',
      render: (status) => <StatusBadge status={status} type="registration" />,
    },
    {
      title: 'Mã QR',
      dataIndex: 'qrCode',
      key: 'qrCode',
      render: (value) => (
        <Space>
          <QrcodeOutlined />
          <Typography.Text copyable>{value}</Typography.Text>
        </Space>
      ),
    },
  ];

  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={registrations}
      loading={loading}
      scroll={{ x: 1100 }}
      pagination={{ pageSize: 8 }}
    />
  );
}
