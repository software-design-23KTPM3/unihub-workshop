import { Button, Card, DatePicker, Form, Input, Select } from 'antd';

const statusOptions = [
  { value: 'OPEN', label: 'OPEN' },
  { value: 'FULL', label: 'FULL' },
  { value: 'CANCELLED', label: 'CANCELLED' },
];

const priceOptions = [
  { value: 'all', label: 'Tất cả' },
  { value: 'free', label: 'Miễn phí' },
  { value: 'paid', label: 'Có phí' },
];

export default function WorkshopFilterBar({ topics, rooms, loading, onChange }) {
  const [form] = Form.useForm();

  const handleValuesChange = (_, values) => {
    onChange({
      keyword: values.keyword,
      date: values.date?.format('YYYY-MM-DD'),
      topic: values.topic,
      room: values.room,
      status: values.status,
      isPaid:
        values.priceType === 'paid' ? true : values.priceType === 'free' ? false : undefined,
    });
  };

  const handleReset = () => {
    form.resetFields();
    onChange({});
  };

  return (
    <Card className="filter-card" bordered={false}>
      <Form
        form={form}
        layout="vertical"
        initialValues={{ priceType: 'all' }}
        onValuesChange={handleValuesChange}
      >
        <div className="workshop-filter-grid">
          <Form.Item label="Tìm kiếm" name="keyword">
            <Input.Search placeholder="Tên workshop, diễn giả, tag..." allowClear />
          </Form.Item>
          <Form.Item label="Ngày" name="date">
            <DatePicker className="full-width" format="DD/MM/YYYY" />
          </Form.Item>
          <Form.Item label="Chủ đề" name="topic">
            <Select allowClear options={topics.map((topic) => ({ value: topic, label: topic }))} />
          </Form.Item>
          <Form.Item label="Phòng" name="room">
            <Select allowClear options={rooms.map((room) => ({ value: room, label: room }))} />
          </Form.Item>
          <Form.Item label="Trạng thái" name="status">
            <Select allowClear options={statusOptions} />
          </Form.Item>
          <Form.Item label="Chi phí" name="priceType">
            <Select options={priceOptions} />
          </Form.Item>
          <Form.Item label=" " className="filter-reset">
            <Button onClick={handleReset} disabled={loading} block>
              Xóa lọc
            </Button>
          </Form.Item>
        </div>
      </Form>
    </Card>
  );
}
