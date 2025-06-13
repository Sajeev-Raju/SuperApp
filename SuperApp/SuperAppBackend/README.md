# User Registration and ID System

This project consists of two Spring Boot microservices that handle user registration, login, and ID generation with payment processing.

## Services

1. **Registration and Login Service**: Handles user registration, OTP verification, and login
2. **ID Generation Service**: Handles ID generation, username validation, and payment processing

## Features

- Multi-factor OTP verification (Email + WhatsApp)
- Fancy username pattern detection with pricing
- Payment integration with Razorpay
- WhatsApp login using Twilio

## Technical Stack

- Java 17
- Spring Boot
- Spring JDBC
- SQLite
- JavaMailSender
- Twilio WhatsApp API
- Razorpay SDK
- SLF4J + Logback

## Setup and Running

### Prerequisites

- Java 17 JDK
- Maven

### Registration and Login Service

1. Configure application.properties with your email and Twilio credentials
2. Run the service:
```bash
cd registration-login-service
mvn spring-boot:run
```

The service will run on port 8080.

### ID Generation Service

1. Configure application.properties with your Razorpay credentials
2. Run the service:
```bash
cd id-generation-service
mvn spring-boot:run
```

The service will run on port 8081.

## API Documentation

### Registration and Login Service

#### Start Registration
- **URL**: `POST /api/register/start`
- **Payload**:
```json
{
  "email": "user@example.com",
  "phone": "+919876543210"
}
```
- **Response**:
```json
{
  "success": true,
  "message": "OTP sent to your email and phone",
  "data": null
}
```

#### Verify OTP
- **URL**: `POST /api/register/verify-otp`
- **Payload**:
```json
{
  "email": "user@example.com",
  "phone": "+919876543210",
  "emailOtp": "123456",
  "phoneOtp": "654321"
}
```
- **Response**:
```json
{
  "success": true,
  "message": "OTP verified successfully",
  "data": null
}
```

#### Validate Username
- **URL**: `POST /api/register/validate-username`
- **Payload**:
```json
{
  "email": "user@example.com",
  "phone": "+919876543210",
  "username": "ABC123"
}
```
- **Response**:
```json
{
  "success": true,
  "message": "Username validation successful",
  "data": {
    "username": "ABC123",
    "available": true,
    "isFancy": true,
    "fancyType": "Sequential Characters",
    "basePrice": 100.0,
    "fancyPrice": 50.0,
    "totalPrice": 150.0
  }
}
```

#### Initiate Payment
- **URL**: `POST /api/register/initiate-payment`
- **Payload**:
```json
{
  "email": "user@example.com",
  "phone": "+919876543210",
  "username": "ABC123",
  "isFancy": true,
  "fancyType": "Sequential Characters",
  "basePrice": 100.0,
  "fancyPrice": 50.0,
  "totalPrice": 150.0
}
```
- **Response**:
```json
{
  "success": true,
  "message": "Payment initiated",
  "data": "https://checkout.razorpay.com/v1/checkout.js?key=..."
}
```

#### Complete Registration
- **URL**: `POST /api/register/complete`
- **Parameters**:
  - `email`: user@example.com
  - `phone`: +919876543210
  - `username`: ABC123
- **Response**:
```json
{
  "success": true,
  "message": "Registration completed successfully",
  "data": null
}
```

#### Login
- **URL**: `POST /api/login/send-otp`
- **Payload**:
```json
{
  "username": "ABC123"
}
```
- **Response**:
```json
{
  "success": true,
  "message": "OTP sent to your WhatsApp",
  "data": null
}
```

#### Verify Login OTP
- **URL**: `POST /api/login/verify`
- **Payload**:
```json
{
  "username": "ABC123",
  "otp": "123456"
}
```
- **Response**:
```json
{
  "success": true,
  "message": "Login successful",
  "data": "user_1_1623847290123"
}
```

### ID Generation Service

#### Generate Username
- **URL**: `GET /api/id-generation/generate`
- **Parameters** (optional):
  - `prefix`: ABC
  - `suffix`: 123
- **Response**:
```json
{
  "success": true,
  "message": "Username generated successfully",
  "data": "ABC123"
}
```

#### Check Username
- **URL**: `POST /api/id-generation/check`
- **Payload**:
```json
{
  "username": "ABC123"
}
```
- **Response**:
```json
{
  "success": true,
  "message": "Username checked successfully",
  "data": {
    "username": "ABC123",
    "available": true,
    "isFancy": true,
    "fancyType": "Sequential Characters",
    "fancyPrice": 50.0
  }
}
```

#### Initiate Payment
- **URL**: `POST /api/id-generation/payment/initiate`
- **Payload**:
```json
{
  "email": "user@example.com",
  "phone": "+919876543210",
  "username": "ABC123",
  "isFancy": true,
  "fancyType": "Sequential Characters",
  "basePrice": 100.0,
  "fancyPrice": 50.0,
  "totalPrice": 150.0
}
```
- **Response**:
```json
{
  "success": true,
  "message": "Payment initiated",
  "data": "https://checkout.razorpay.com/v1/checkout.js?key=..."
}
```

#### Verify Payment
- **URL**: `POST /api/id-generation/payment/verify`
- **Parameters**:
  - `paymentId`: pay_123456789
  - `signature`: abcdef123456
- **Response**:
```json
{
  "success": true,
  "message": "Payment verified successfully",
  "data": {
    "orderId": "1",
    "razorpayOrderId": "order_123456789",
    "amount": 150.0,
    "currency": "INR",
    "receipt": "receipt_1",
    "paymentId": "pay_123456789",
    "status": "COMPLETED",
    "username": "ABC123"
  }
}
```

#### Get Payment Status
- **URL**: `GET /api/id-generation/payment/status/{paymentId}`
- **Response**:
```json
{
  "success": true,
  "message": "Payment status retrieved",
  "data": {
    "paymentId": "pay_123456789",
    "status": "COMPLETED",
    "username": "ABC123",
    "amount": 150.0,
    "email": "user@example.com",
    "phone": "+919876543210"
  }
}
```