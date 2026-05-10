import { CalendarOutlined, InboxOutlined } from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  DatePicker,
  Divider,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Switch,
  TimePicker,
  Typography,
  Upload,
  message,
} from 'antd';
import dayjs from 'dayjs';

const WORKSHOP_DATE_PLACEHOLDER = '2026-01-01';
const MAX_PDF_SIZE_MB = 20;
const MAX_PDF_SIZE_BYTES = MAX_PDF_SIZE_MB * 1024 * 1024;

function parseDateTime(value) {
  return value ? dayjs(value) : null;
}

export function toWorkshopFormValues(workshop) {
  if (!workshop) {
    const eventDate = dayjs().add(7, 'day').startOf('day');

    return {
      date: eventDate,
      startTime: dayjs(`${WORKSHOP_DATE_PLACEHOLDER} 08:30`),
      endTime: dayjs(`${WORKSHOP_DATE_PLACEHOLDER} 10:00`),
      registrationStartTime: dayjs().minute(0).second(0).millisecond(0),
      registrationEndTime: eventDate.subtract(1, 'day').hour(23).minute(59),
      isPaid: false,
      price: 0,
      tags: [],
    };
  }

  return {
    ...workshop,
    date: dayjs(workshop.date),
    startTime: dayjs(`${WORKSHOP_DATE_PLACEHOLDER} ${workshop.startTime}`),
    endTime: dayjs(`${WORKSHOP_DATE_PLACEHOLDER} ${workshop.endTime}`),
    registrationStartTime: parseDateTime(workshop.registrationStartTime),
    registrationEndTime: parseDateTime(workshop.registrationEndTime),
    isPaid: Boolean(workshop.isPaid || Number(workshop.price || 0) > 0),
    price: Number(workshop.price || 0),
    tags: workshop.tags || [],
  };
}

export function toWorkshopPayload(values) {
  const isPaid = Boolean(values.isPaid);

  return {
    title: values.title,
    speaker: values.speakerName,
    speakerName: values.speakerName,
    speakerTitle: values.speakerTitle,
    topic: values.topic,
    description: values.description,
    room: values.room,
    roomMapText: values.roomMapText,
    date: values.date.format('YYYY-MM-DD'),
    startTime: values.startTime.format('HH:mm'),
    endTime: values.endTime.format('HH:mm'),
    registrationStartTime: values.registrationStartTime.format('YYYY-MM-DDTHH:mm'),
    registrationEndTime: values.registrationEndTime.format('YYYY-MM-DDTHH:mm'),
    capacity: Number(values.capacity),
    price: isPaid ? Number(values.price || 0) : 0,
    tags: values.tags || [],
    aiSummary: values.aiSummary || null,
    isPaid,
    pdfFile: values.pdfFile?.[0]?.originFileObj,
  };
}

function normalizeUploadEvent(event) {
  return Array.isArray(event) ? event : event?.fileList || [];
}

function validatePdfBeforeUpload(file) {
  const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');

  if (!isPdf) {
    message.error('Chỉ hỗ trợ file PDF.');
    return Upload.LIST_IGNORE;
  }

  if (file.size > MAX_PDF_SIZE_BYTES) {
    message.error(`PDF không được vượt quá ${MAX_PDF_SIZE_MB} MB.`);
    return Upload.LIST_IGNORE;
  }

  return false;
}

export default function WorkshopForm({ initialValues, submitting, submitText, onSubmit }) {
  const [form] = Form.useForm();
  const isPaid = Form.useWatch('isPaid', form);

  const handleUploadChange = ({ fileList }) => {
    if (fileList.length > 0) {
      form.setFieldsValue({
        aiSummary: 'Tóm tắt sẽ được tạo từ PDF đã tải lên.',
      });
    }
  };

  return (
    <Card bordered={false} className="admin-form-card">
      <Form
        form={form}
        layout="vertical"
        initialValues={initialValues}
        onFinish={(values) => onSubmit(toWorkshopPayload(values))}
        requiredMark={false}
      >
        <Alert
          className="admin-form-alert"
          type="info"
          showIcon
          message="Khoảng mở đăng ký tách riêng với lịch diễn ra"
          description="Sinh viên chỉ đăng ký được trong thời gian mở đăng ký. Ngày, giờ bắt đầu và giờ kết thúc bên dưới là lịch diễn ra workshop."
        />

        <Divider orientation="left" orientationMargin={0}>
          Thông tin chính
        </Divider>

        <div className="admin-form-grid">
          <Form.Item
            label="Tên workshop"
            name="title"
            rules={[{ required: true, message: 'Vui lòng nhập tên workshop.' }]}
          >
            <Input placeholder="VD: Phỏng vấn kỹ thuật Java" />
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
        </div>

        <Divider orientation="left" orientationMargin={0}>
          Lịch diễn ra
        </Divider>

        <div className="admin-form-grid admin-form-grid--time">
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
        </div>

        <Divider orientation="left" orientationMargin={0}>
          Khoảng mở đăng ký
        </Divider>

        <div className="admin-form-grid admin-form-grid--time">
          <Form.Item
            label="Mở đăng ký"
            name="registrationStartTime"
            rules={[{ required: true, message: 'Vui lòng chọn thời điểm mở đăng ký.' }]}
          >
            <DatePicker
              className="full-width"
              showTime={{ format: 'HH:mm', minuteStep: 5 }}
              format="DD/MM/YYYY HH:mm"
              suffixIcon={<CalendarOutlined />}
            />
          </Form.Item>

          <Form.Item
            label="Đóng đăng ký"
            name="registrationEndTime"
            dependencies={['registrationStartTime', 'date', 'startTime']}
            rules={[
              { required: true, message: 'Vui lòng chọn thời điểm đóng đăng ký.' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  const registrationStart = getFieldValue('registrationStartTime');
                  const eventDate = getFieldValue('date');
                  const eventStart = getFieldValue('startTime');

                  if (!value || !registrationStart) {
                    return Promise.resolve();
                  }

                  if (!value.isAfter(registrationStart)) {
                    return Promise.reject(new Error('Đóng đăng ký phải sau thời điểm mở đăng ký.'));
                  }

                  if (eventDate && eventStart) {
                    const workshopStart = dayjs(
                      `${eventDate.format('YYYY-MM-DD')} ${eventStart.format('HH:mm')}`,
                    );
                    if (value.isAfter(workshopStart)) {
                      return Promise.reject(
                        new Error('Đóng đăng ký phải trước hoặc đúng giờ bắt đầu workshop.'),
                      );
                    }
                  }

                  return Promise.resolve();
                },
              }),
            ]}
          >
            <DatePicker
              className="full-width"
              showTime={{ format: 'HH:mm', minuteStep: 5 }}
              format="DD/MM/YYYY HH:mm"
              suffixIcon={<CalendarOutlined />}
            />
          </Form.Item>
        </div>

        <Divider orientation="left" orientationMargin={0}>
          Chi phí và phân loại
        </Divider>

        <div className="admin-form-grid admin-form-grid--time">
          <Form.Item label="Workshop có phí" name="isPaid" valuePropName="checked">
            <Switch checkedChildren="Có phí" unCheckedChildren="Miễn phí" />
          </Form.Item>

          <Form.Item
            label="Giá"
            name="price"
            rules={[
              { type: 'number', min: 0, message: 'Giá phải lớn hơn hoặc bằng 0.' },
              () => ({
                validator(_, value) {
                  if (!isPaid || Number(value || 0) > 0) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('Workshop có phí phải có giá lớn hơn 0.'));
                },
              }),
            ]}
          >
            <InputNumber
              className="full-width"
              min={0}
              step={10000}
              disabled={!isPaid}
              addonAfter="VND"
            />
          </Form.Item>

          <Form.Item label="Tags" name="tags">
            <Select mode="tags" tokenSeparators={[',']} placeholder="Java, Interview" />
          </Form.Item>
        </div>

        <Divider orientation="left" orientationMargin={0}>
          Nội dung
        </Divider>

        <Form.Item label="Mô tả" name="description">
          <Input.TextArea rows={4} />
        </Form.Item>

        <Form.Item label="Sơ đồ phòng dạng mô tả" name="roomMapText">
          <Input.TextArea rows={2} />
        </Form.Item>

        <Form.Item
          label="PDF giới thiệu"
          name="pdfFile"
          valuePropName="fileList"
          getValueFromEvent={normalizeUploadEvent}
        >
          <Upload.Dragger
            beforeUpload={validatePdfBeforeUpload}
            maxCount={1}
            accept=".pdf"
            onChange={handleUploadChange}
          >
            <p className="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p className="ant-upload-text">Chọn PDF mô tả workshop</p>
            <p className="ant-upload-hint">PDF tối đa {MAX_PDF_SIZE_MB} MB. Hệ thống sẽ tạo phần tóm tắt để hiển thị cho sinh viên.</p>
          </Upload.Dragger>
        </Form.Item>

        <Form.Item label="Tóm tắt tự động" name="aiSummary">
          <Input.TextArea rows={3} placeholder="Tóm tắt nội dung workshop sẽ hiển thị tại trang chi tiết." />
        </Form.Item>

        <Space className="admin-form-actions">
          <Button type="primary" htmlType="submit" loading={submitting}>
            {submitText}
          </Button>
          <Typography.Text type="secondary">
            Thay đổi lịch, sức chứa và thời gian đăng ký sẽ có hiệu lực sau khi lưu.
          </Typography.Text>
        </Space>
      </Form>
    </Card>
  );
}
