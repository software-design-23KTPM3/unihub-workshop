import { InboxOutlined } from '@ant-design/icons';
import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  TimePicker,
  Upload,
} from 'antd';
import dayjs from 'dayjs';
import { useState } from 'react';

const statusOptions = [
  { value: 'OPEN', label: 'OPEN' },
  { value: 'FULL', label: 'FULL' },
  { value: 'CANCELLED', label: 'CANCELLED' },
];

export function toWorkshopFormValues(workshop) {
  if (!workshop) {
    return {
      status: 'OPEN',
      price: 0,
      tags: [],
    };
  }

  return {
    ...workshop,
    date: dayjs(workshop.date),
    startTime: dayjs(`2026-01-01 ${workshop.startTime}`),
    endTime: dayjs(`2026-01-01 ${workshop.endTime}`),
  };
}

export function toWorkshopPayload(values) {
  return {
    title: values.title,
    speakerName: values.speakerName,
    speakerTitle: values.speakerTitle,
    topic: values.topic,
    description: values.description,
    room: values.room,
    roomMapText: values.roomMapText,
    date: values.date.format('YYYY-MM-DD'),
    startTime: values.startTime.format('HH:mm'),
    endTime: values.endTime.format('HH:mm'),
    capacity: Number(values.capacity),
    price: Number(values.price || 0),
    tags: values.tags || [],
    status: values.status || 'OPEN',
    aiSummary: values.aiSummary || '',
    isPaid: Number(values.price || 0) > 0,
  };
}

export default function WorkshopForm({ initialValues, submitting, submitText, onSubmit }) {
  const [form] = Form.useForm();
  const [pdfFile, setPdfFile] = useState(null);

  const handleUploadChange = ({ fileList }) => {
    setPdfFile(fileList[0]?.originFileObj || null);
  };

  return (
    <Card bordered={false} className="admin-form-card">
      <Form
        form={form}
        layout="vertical"
        initialValues={initialValues}
        onFinish={(values) => onSubmit(toWorkshopPayload(values), pdfFile)}
        requiredMark={false}
      >
        <div className="admin-form-grid">
          <Form.Item
            label="Tên workshop"
            name="title"
            rules={[{ required: true, message: 'Vui lòng nhập tên workshop.' }]}
          >
            <Input placeholder="VD: Phỏng vấn kỹ thuật Java Backend" />
          </Form.Item>

          <Form.Item label="Chủ đề" name="topic">
            <Input placeholder="Technical Interview" />
          </Form.Item>

          <Form.Item label="Tên diễn giả" name="speakerName">
            <Input />
          </Form.Item>

          <Form.Item label="Chức danh diễn giả" name="speakerTitle">
            <Input />
          </Form.Item>

          <Form.Item label="Phòng" name="room">
            <Input />
          </Form.Item>

          <Form.Item
            label="Sức chứa"
            name="capacity"
            rules={[
              { required: true, message: 'Vui lòng nhập sức chứa.' },
              { type: 'number', min: 1, message: 'Sức chứa phải lớn hơn 0.' },
            ]}
          >
            <InputNumber className="full-width" min={1} />
          </Form.Item>

          <Form.Item
            label="Ngày"
            name="date"
            rules={[{ required: true, message: 'Vui lòng chọn ngày.' }]}
          >
            <DatePicker className="full-width" format="DD/MM/YYYY" />
          </Form.Item>

          <Form.Item
            label="Giờ bắt đầu"
            name="startTime"
            rules={[{ required: true, message: 'Vui lòng chọn giờ bắt đầu.' }]}
          >
            <TimePicker className="full-width" format="HH:mm" minuteStep={5} />
          </Form.Item>

          <Form.Item
            label="Giờ kết thúc"
            name="endTime"
            rules={[{ required: true, message: 'Vui lòng chọn giờ kết thúc.' }]}
          >
            <TimePicker className="full-width" format="HH:mm" minuteStep={5} />
          </Form.Item>

          <Form.Item
            label="Giá"
            name="price"
            rules={[{ type: 'number', min: 0, message: 'Giá phải lớn hơn hoặc bằng 0.' }]}
          >
            <InputNumber className="full-width" min={0} step={10000} />
          </Form.Item>

          <Form.Item label="Trạng thái" name="status">
            <Select options={statusOptions} />
          </Form.Item>

          <Form.Item label="Tags" name="tags">
            <Select mode="tags" tokenSeparators={[',']} placeholder="Java, Interview" />
          </Form.Item>
        </div>

        <Form.Item label="Mô tả" name="description">
          <Input.TextArea rows={4} />
        </Form.Item>

        <Form.Item label="Sơ đồ phòng dạng mô tả" name="roomMapText">
          <Input.TextArea rows={2} />
        </Form.Item>

        <Form.Item label="Upload PDF">
          <Upload.Dragger
            beforeUpload={() => false}
            maxCount={1}
            accept=".pdf"
            onChange={handleUploadChange}
            onRemove={() => {
              setPdfFile(null);
            }}
          >
            <p className="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p className="ant-upload-text">Chọn PDF mô tả workshop</p>
            <p className="ant-upload-hint">File sẽ được gửi để backend tạo AI Summary sau khi lưu workshop.</p>
          </Upload.Dragger>
        </Form.Item>

        <Form.Item label="AI Summary" name="aiSummary">
          <Input.TextArea rows={3} placeholder="AI Summary sẽ được cập nhật sau khi worker xử lý PDF" />
        </Form.Item>

        <Space>
          <Button type="primary" htmlType="submit" loading={submitting}>
            {submitText}
          </Button>
        </Space>
      </Form>
    </Card>
  );
}
