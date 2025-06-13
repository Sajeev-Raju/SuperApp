export interface ApiResponse<T = any> {
  success: boolean;
  message: string;
  data: T;
}

export interface RegistrationData {
  email: string;
  phone: string;
}

export interface OtpVerificationData {
  email: string;
  phone: string;
  emailOtp: string;
  phoneOtp: string;
}

export interface UsernameValidationData {
  email: string;
  phone: string;
  username?: string;
}

export interface PaymentData {
  email: string;  // Must be valid email format
  phone: string;  // Must match pattern: ^\+?[0-9]{10,15}$
  username: string;  // Required, not blank
  isFancy: boolean;
  fancyType: string | null;  // Optional
  basePrice: number;  // Required, not null
  fancyPrice: number;  // Optional
  totalPrice: number;  // Required, not null
}

export interface LoginData {
  username: string;
}

export interface LoginOtpVerificationData {
  username: string;
  otp: string;
}

export interface SessionValidationData {
  username: string;
  sessionId: string;
}

export interface UsernameValidationResult {
  username: string;
  available: boolean;
  isFancy: boolean;
  fancyType: string;
  basePrice: number;
  fancyPrice: number;
  totalPrice: number;
}

export interface QRPaymentResponse {
  qrImageUrl: string;
  razorpayOrderId: string;
}

export interface PaymentInitiateResponse {
  upiPaymentLink: string;
  paymentLinkId: string;
}

export interface PaymentStatusResponse {
  orderId: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
  username: string;
  amount: number;
  email: string;
  phone: string;
}

export interface PaymentStatusCheckParams {
  paymentLinkId: string;
}