# UniHub Workshop Frontend API Contract

Base URL:

```env
VITE_API_BASE_URL=http://localhost/api
```

Authentication:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Frontend hỗ trợ mock mode với `VITE_USE_MOCK=true`. Khi `VITE_USE_MOCK=false`, service sẽ gọi các endpoint dưới đây qua `src/services/httpClient.js`.

## Auth

### POST `/auth/login`

Request:

```json
{
  "email": "student@example.com",
  "password": "123456"
}
```

Response:

```json
{
  "token": "jwt-access-token",
  "user": {
    "id": "student-001",
    "studentId": "2312345",
    "name": "Sinh viên UniHub",
    "email": "student@example.com",
    "role": "STUDENT"
  }
}
```

Role frontend đang dùng:

- `STUDENT`
- `ORGANIZER`

## Workshops

### GET `/workshops`

Query params frontend có thể gửi:

```text
keyword=Java
date=2026-05-06
topic=Technical Interview
room=B203
status=OPEN
isPaid=true
```

Response:

```json
[
  {
    "id": "ws-java-interview-02",
    "title": "Phỏng vấn kỹ thuật Java Backend",
    "speakerName": "Trần Hoàng Nam",
    "speakerTitle": "Backend Lead, VNG Cloud",
    "topic": "Technical Interview",
    "description": "Mô phỏng phỏng vấn Java Backend...",
    "aiSummary": "Người tham gia nắm được cấu trúc...",
    "room": "B203",
    "roomMapText": "Tầng 2, tòa B...",
    "date": "2026-05-06",
    "startTime": "10:30",
    "endTime": "12:00",
    "capacity": 60,
    "registeredCount": 60,
    "price": 50000,
    "status": "FULL",
    "tags": ["Java", "Spring Boot", "Interview"],
    "isPaid": true
  }
]
```

### GET `/workshops/:id`

Response: một workshop object cùng shape với `GET /workshops`.

## Student Registrations

### POST `/registrations`

Request:

```json
{
  "workshopId": "ws-cv-it-01"
}
```

Response:

```json
{
  "id": "reg-001",
  "studentId": "2312345",
  "workshopId": "ws-cv-it-01",
  "status": "REGISTERED",
  "qrCode": "UNIHUB-REG-001-2312345-WS-CV",
  "registeredAt": "2026-04-28T09:12:00+07:00",
  "paymentStatus": "FREE",
  "workshop": {
    "id": "ws-cv-it-01",
    "title": "Viết CV cho sinh viên IT"
  }
}
```

### GET `/me/registrations`

Response:

```json
[
  {
    "id": "reg-001",
    "studentId": "2312345",
    "workshopId": "ws-cv-it-01",
    "status": "REGISTERED",
    "qrCode": "UNIHUB-REG-001-2312345-WS-CV",
    "registeredAt": "2026-04-28T09:12:00+07:00",
    "paymentStatus": "FREE",
    "workshop": {
      "id": "ws-cv-it-01",
      "title": "Viết CV cho sinh viên IT",
      "room": "A101",
      "date": "2026-05-06",
      "startTime": "08:30",
      "endTime": "10:00",
      "price": 0
    }
  }
]
```

### GET `/registrations/:id`

Response: một registration object có embedded workshop details.

### POST `/registrations/:id/payment/mock-success`

Endpoint tạm cho demo thanh toán thành công khi payment thật chưa có.

Response:

```json
{
  "id": "reg-002",
  "status": "PAID",
  "paymentStatus": "PAID"
}
```

## Admin Workshops

### GET `/admin/workshops`

Query params và response shape giống `GET /workshops`.

### POST `/admin/workshops`

Request:

```json
{
  "title": "DevOps và CI/CD cơ bản",
  "speakerName": "Hoàng Gia Bảo",
  "speakerTitle": "DevOps Engineer",
  "topic": "DevOps",
  "description": "Trình bày Git workflow...",
  "room": "Lab DevOps",
  "roomMapText": "Tầng 4, tòa E",
  "date": "2026-05-08",
  "startTime": "08:00",
  "endTime": "10:30",
  "capacity": 50,
  "price": 60000,
  "tags": ["DevOps", "Docker", "CI/CD"],
  "status": "OPEN",
  "aiSummary": "AI Summary generated from uploaded PDF",
  "isPaid": true
}
```

Response: created workshop object.

### PUT `/admin/workshops/:id`

Request: cùng shape với create payload.

Response: updated workshop object.

### PATCH `/admin/workshops/:id/cancel`

Response:

```json
{
  "id": "ws-devops-cicd-06",
  "status": "CANCELLED"
}
```

## Admin Registrations

### GET `/admin/registrations`

Query params:

```text
workshopId=ws-cv-it-01
status=REGISTERED
```

Response:

```json
[
  {
    "id": "reg-001",
    "studentId": "2312345",
    "studentName": "Sinh viên UniHub",
    "studentEmail": "student@example.com",
    "workshopId": "ws-cv-it-01",
    "workshop": {
      "id": "ws-cv-it-01",
      "title": "Viết CV cho sinh viên IT"
    },
    "status": "REGISTERED",
    "paymentStatus": "FREE",
    "qrCode": "UNIHUB-REG-001-2312345-WS-CV",
    "registeredAt": "2026-04-28T09:12:00+07:00"
  }
]
```

## Admin Statistics

### GET `/admin/statistics`

Endpoint này đã được khai báo để backend có thể trả thống kê tổng hợp. UI hiện tại vẫn có thể tự tổng hợp từ workshop và registration nếu mock mode đang bật.

Response gợi ý:

```json
{
  "totalWorkshops": 12,
  "openWorkshops": 8,
  "totalRegistrations": 1284,
  "fullWorkshops": 2,
  "registrationCountByWorkshop": [
    {
      "workshopId": "ws-cv-it-01",
      "title": "Viết CV cho sinh viên IT",
      "count": 52
    }
  ],
  "paidFreeDistribution": {
    "paid": 320,
    "free": 920,
    "pending": 44
  }
}
```

## Error Format

Recommended error response:

```json
{
  "message": "Workshop đã hết chỗ.",
  "code": "WORKSHOP_FULL"
}
```
