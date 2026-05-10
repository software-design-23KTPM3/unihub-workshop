# Frontend - UniHub Workshop

## 1. Giới Thiệu

Frontend này là Web UI cho hệ thống UniHub Workshop, dùng để demo và vận hành tuần lễ workshop kỹ năng, hướng nghiệp và nghề nghiệp của trường đại học.

Ứng dụng hiện hỗ trợ 2 role:

- `STUDENT`: xem danh sách workshop, xem chi tiết, đăng ký workshop, xem QR ticket và danh sách đã đăng ký.
- `ORGANIZER`: quản lý workshop, tạo/sửa/hủy workshop, xem danh sách đăng ký và thống kê.

Backend có thể chưa hoàn chỉnh, nên frontend hỗ trợ chế độ mock data để chạy độc lập. Khi backend sẵn sàng, có thể chuyển sang gọi API thật bằng biến môi trường và service layer.

## 2. Công Nghệ Sử Dụng

Theo `package.json`, frontend đang dùng:

- React
- Vite
- React Router DOM
- Ant Design
- `@ant-design/icons`
- `dayjs`
- Native `fetch` thông qua wrapper `src/services/httpClient.js`

## 3. Cấu Trúc Thư Mục Frontend

```text
src/app-web/
├── docs/
│   └── api-contract.md      # API contract frontend kỳ vọng từ backend
├── src/
│   ├── app/                 # App root, router
│   ├── components/          # Component tái sử dụng
│   │   ├── admin/
│   │   ├── common/
│   │   └── workshop/
│   ├── config/              # Cấu hình API/env/theme
│   ├── contexts/            # AuthContext
│   ├── guards/              # RoleGuard bảo vệ route theo role
│   ├── hooks/               # Custom hooks, ví dụ useAuth
│   ├── layouts/             # Layout riêng cho auth/student/admin
│   ├── mocks/               # Mock data khi VITE_USE_MOCK=true
│   ├── pages/               # Page theo nhóm role
│   │   ├── admin/
│   │   ├── auth/
│   │   └── student/
│   ├── services/            # Service layer gọi mock hoặc API thật
│   ├── styles/              # Theme/style dùng chung
│   └── utils/               # Hàm tiện ích
├── .env.example
├── index.html
├── package.json
├── package-lock.json
└── vite.config.js
```

## 4. Cách Cài Đặt Và Chạy Frontend

Từ thư mục gốc project:

```bash
cd frontend
npm install
npm run dev
```

URL mặc định theo `vite.config.js`:

```text
http://localhost:5173
```

Kiểm tra build production:

```bash
npm run build
```

## 5. Cấu Hình Biến Môi Trường

Frontend có file mẫu:

```text
src/app-web/.env.example
```

Nội dung:

```env
VITE_USE_MOCK=true
VITE_API_BASE_URL=http://localhost/api
```

Khi chạy local, tạo hoặc sửa file:

```text
src/app-web/.env
```

Ý nghĩa:

- `VITE_USE_MOCK=true`: frontend dùng mock data, không cần backend.
- `VITE_USE_MOCK=false`: frontend gọi backend API thật.
- `VITE_API_BASE_URL`: base URL của backend API.

Code đọc cấu hình tại `src/config/api.js`. Vite không tự đọc `.env.example`, nên cần có `.env` nếu muốn cấu hình có hiệu lực khi chạy local.

## 6. Cách Chạy Bằng Mock Data

1. Tạo hoặc mở file `src/app-web/.env`.
2. Đặt:

```env
VITE_USE_MOCK=true
VITE_API_BASE_URL=http://localhost/api
```

3. Chạy:

```bash
npm run dev
```

4. Đăng nhập bằng tài khoản demo.
5. Frontend sẽ lấy dữ liệu từ `src/mocks/` thông qua service layer trong `src/services/`.

Tài khoản demo hiện có trong `src/mocks/users.mock.js`:

```text
student@example.com / 123456 / STUDENT
admin@example.com / 123456 / ORGANIZER
```

## 7. Cách Chuyển Từ Mock Sang Gọi API Thật

Bước 1: mở file `src/app-web/.env` và đổi:

```env
VITE_USE_MOCK=false
```

Bước 2: cấu hình backend URL:

```env
VITE_API_BASE_URL=http://localhost/api
```

Bước 3: đảm bảo backend đang chạy đúng URL trên.

Bước 4: restart dev server:

```bash
Ctrl + C
npm run dev
```

Bước 5: kiểm tra service layer trong `src/services/`.

Component không gọi mock trực tiếp. Component gọi service, service sẽ quyết định dùng mock hay API thật dựa trên `VITE_USE_MOCK`.

Nếu API backend khác contract hiện tại, cần sửa tại:

- `src/services/apiEndpoints.js`
- `src/services/httpClient.js`
- `src/services/authService.js`
- `src/services/workshopService.js`
- `src/services/registrationService.js`

Hiện chưa có service riêng tên `statisticsService`; trang thống kê admin đang tổng hợp từ `workshopService` và `registrationService`. Endpoint `/admin/statistics` đã được khai báo trong `apiEndpoints.js` để dành cho backend API thật nếu cần.

## 8. Luồng Hoạt Động Khi Frontend Gọi API

```text
User action
  ↓
React Page / Component
  ↓
Service Layer
  ↓
Kiểm tra VITE_USE_MOCK
  ↓
Mock Data hoặc Backend API
  ↓
Response
  ↓
Update UI
```

Chi tiết:

```text
Component Page
→ gọi service function
→ service kiểm tra VITE_USE_MOCK
→ nếu true: lấy dữ liệu từ mocks/mockStore
→ nếu false: gọi httpClient
→ httpClient gửi request đến VITE_API_BASE_URL
→ backend trả response
→ service trả dữ liệu về component
→ component render UI
```

## 9. Các API Frontend Kỳ Vọng Từ Backend

Danh sách dưới đây lấy từ `src/services/apiEndpoints.js`.

Auth:

- `POST /auth/login`

Workshop:

- `GET /workshops`
- `GET /workshops/:id`

Registration:

- `POST /registrations`
- `GET /me/registrations`
- `GET /registrations/:id`
- `POST /registrations/:id/payment/mock-success`

Admin/Organizer:

- `GET /admin/workshops`
- `POST /admin/workshops`
- `PUT /admin/workshops/:id`
- `PATCH /admin/workshops/:id/cancel`
- `GET /admin/registrations`
- `GET /admin/statistics`

API contract chi tiết nằm tại `src/app-web/docs/api-contract.md`.

## 10. Các Nơi Cần Sửa Khi Backend API Khác Mock

Checklist khi nối backend:

- Backend trả `workshop_name` nhưng UI dùng `title` → sửa mapper trong `workshopService`.
- Backend trả `start_time` nhưng UI dùng `startTime` → sửa mapper trong `workshopService`.
- Backend trả `registered_count` nhưng UI dùng `registeredCount` → sửa mapper trong `workshopService`.
- Backend trả role là `organizer` nhưng frontend dùng `ORGANIZER` → chuẩn hóa trong `authService`.
- Backend dùng JWT → sửa `authService` để lưu `accessToken` hoặc `token`.
- Backend yêu cầu `Authorization` header → kiểm tra `httpClient`; hiện đã có logic gắn `Bearer token` nếu user trong localStorage có `token` hoặc `accessToken`.
- Backend endpoint khác → sửa `apiEndpoints.js`.
- Backend response bọc trong `{ data: ... }` → sửa `httpClient` hoặc từng service để lấy `response.data`.
- Backend lỗi trả format khác `{ message, code }` → sửa xử lý lỗi trong `httpClient`.
- Backend không hỗ trợ endpoint payment mock → thay `registrations.mockPaymentSuccess` bằng endpoint payment thật hoặc tách payment service.

## 11. Luồng Demo Theo Từng Role

Student:

```text
Login
→ xem danh sách workshop
→ xem chi tiết workshop
→ đăng ký workshop
→ nhận QR ticket
→ xem Đăng ký của tôi
```

Route chính:

```text
/student/workshops
/student/workshops/:id
/student/my-registrations
/student/tickets/:registrationId
```

Organizer:

```text
Login
→ vào dashboard
→ quản lý workshop
→ tạo/sửa/hủy workshop
→ xem danh sách đăng ký
→ xem thống kê
```

Route chính:

```text
/admin/dashboard
/admin/workshops
/admin/workshops/create
/admin/workshops/:id/edit
/admin/registrations
/admin/statistics
```

## 12. Kiểm Tra Sau Khi Chuyển Sang API Thật

Checklist test:

- Login bằng tài khoản thật.
- Kiểm tra role redirect đúng:
  - `STUDENT` → `/student/workshops`
  - `ORGANIZER` → `/admin/dashboard`
- Student load được danh sách workshop từ backend.
- Student xem được chi tiết workshop.
- Student đăng ký workshop thành công.
- QR ticket hiển thị đúng.
- Organizer load được dashboard.
- Organizer load được danh sách workshop.
- Organizer tạo workshop được.
- Organizer sửa workshop được.
- Organizer hủy workshop được.
- Organizer xem được danh sách đăng ký.
- Organizer xem được thống kê.
- Khi backend tắt, UI không trắng màn hình mà hiển thị lỗi thân thiện.
- Console không có lỗi nghiêm trọng như `ERR_CONNECTION_REFUSED`, `401` không xử lý, hoặc exception làm crash UI.
