// src/lib/api.ts
import axios from 'axios';
import type { 
  ApiResponse,
  RegistrationData,
  OtpVerificationData,
  UsernameValidationData,
  PaymentData,
  LoginData,
  LoginOtpVerificationData,
  SessionValidationData,
  UsernameValidationResult,
  PaymentInitiateResponse,
  PaymentStatusResponse,
  PaymentStatusCheckParams
} from './types';

const API_URL = 'http://localhost:8080/api';
const ID_GEN_URL = 'http://localhost:8081/api/id-generation';




// Create axios instance for main API
const apiInstance = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  withCredentials: true,
  timeout: 10000
});

// Create axios instance for ID generation API
const idGenInstance = axios.create({
  baseURL: ID_GEN_URL,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true,
  timeout: 10000
});

// Add CORS headers to all requests
apiInstance.interceptors.request.use(
  (config) => {
    config.headers['Access-Control-Allow-Origin'] = '*';
    config.headers['Access-Control-Allow-Methods'] = 'GET, POST, PUT, DELETE, OPTIONS';
    config.headers['Access-Control-Allow-Headers'] = 'Content-Type, Authorization';
    
    console.log('🚀 API Request:', {
      method: config.method?.toUpperCase(),
      url: config.url,
      baseURL: config.baseURL,
      fullURL: `${config.baseURL}${config.url}`
    });
    return config;
  },
  (error) => {
    console.error('❌ Request Error:', error);
    return Promise.reject(error);
  }
);

// Add response interceptor for debugging
apiInstance.interceptors.response.use(
  (response) => {
    console.log('✅ API Response:', {
      status: response.status,
      url: response.config.url,
      data: response.data
    });
    return response;
  },
  (error) => {
    console.error('❌ API Error:', {
      status: error.response?.status,
      message: error.message,
      url: error.config?.url,
      response: error.response?.data
    });
    
    // Handle CORS errors specifically
    if (error.code === 'ERR_NETWORK') {
      console.error('🚫 Network Error - Check if backend is running and CORS is configured');
    }
    
    return Promise.reject(error);
  }
);

// Apply same interceptors to ID generation instance
idGenInstance.interceptors.request.use(
  apiInstance.interceptors.request.handlers[0].fulfilled,
  apiInstance.interceptors.request.handlers[0].rejected
);

idGenInstance.interceptors.response.use(
  apiInstance.interceptors.response.handlers[0].fulfilled,
  apiInstance.interceptors.response.handlers[0].rejected
);

export const api = {
  // Registration endpoints
  startRegistration: async (data: RegistrationData) => {
    return await apiInstance.post<ApiResponse>('/register/start', data);
  },
  
  verifyOtp: async (data: OtpVerificationData) => {
    return await apiInstance.post<ApiResponse>('/register/verify-otp', data);
  },
  
  validateUsername: async (data: UsernameValidationData) => {
    return await apiInstance.post<ApiResponse<UsernameValidationResult>>('/register/validate-username', data);
  },
  
  initiateQRPayment: async (data: PaymentData) => {
    return await idGenInstance.post<ApiResponse<PaymentInitiateResponse>>('/payment/initiate', data);
  },

  checkPaymentStatus: async (paymentLinkId: string) => {
    return await idGenInstance.get<ApiResponse<PaymentStatusResponse>>('/payment/status', {
      params: { paymentId: paymentLinkId }
    });
  },
  
  completeRegistration: async (params: { email: string; phone: string; username: string }) => {
    return await apiInstance.post<ApiResponse>('/register/complete', null, { params });
  },

  // Login endpoints
  login: async (data: LoginData) => {
    return await apiInstance.post<ApiResponse>('/login/send-otp', data);
  },
  
  verifyLoginOtp: async (data: LoginOtpVerificationData) => {
    return await apiInstance.post<ApiResponse<string>>('/login/verify', data);
  },
  
  continueWithOldestLogout: async (data: LoginData) => {
    return await apiInstance.post<ApiResponse>('/login/continue-with-oldest-logout', data);
  },
  
  logout: async (params: { username: string; sessionId: string }) => {
    return await apiInstance.post<ApiResponse>(
      '/login/logout',
      {}, // no body needed
      {
        headers: {
          'username': params.username,
          'session-id': params.sessionId
        }
      }
    );
  },
  

  // Session endpoints
  validateSession: async (data: SessionValidationData) => {
    return await apiInstance.post<ApiResponse>('/session/validate', data);
  },

  // ID Generation endpoints
  generateUsername: async (params?: { prefix?: string; suffix?: string }) => {
    return await idGenInstance.get<ApiResponse<string>>('/generate', { params });
  },
  
  checkUsername: async (data: { username: string }) => {
    return await idGenInstance.post<ApiResponse<UsernameValidationResult>>('/check', data);
  },
  
  getSuggestions: async (headers: { email: string; phone: string }, params: { attempt: number }) => {
    return await idGenInstance.get<ApiResponse>('/suggestions', { headers, params });
  }
};

// Test function for API connectivity
export const testApiConnection = async () => {
  try {
    console.log('🔍 Testing API connections...');
    
    // Test main API with a simple request
    const mainApiTest = await apiInstance.get('/health').catch((error) => {
      console.log('Main API health check failed:', error.message);
      return null;
    });
    
    // Test ID generation API
    const idGenTest = await idGenInstance.get('/health').catch((error) => {
      console.log('ID Gen API health check failed:', error.message);
      return null;
    });
    
    // If health endpoints don't exist, try actual endpoints
    if (!mainApiTest) {
      try {
        await apiInstance.post('/register/start', { email: 'test', phone: 'test' });
      } catch (error: any) {
        // If we get a validation error (400), the API is working
        if (error.response?.status === 400) {
          console.log('✅ Main API is responding (validation error expected)');
        }
      }
    }
    
    if (!idGenTest) {
      try {
        await idGenInstance.get('/generate');
      } catch (error: any) {
        // If we get any response, the API is working
        if (error.response?.status) {
          console.log('✅ ID Gen API is responding');
        }
      }
    }
    
    return {
      mainApi: !!mainApiTest,
      idGenApi: !!idGenTest
    };
  } catch (error) {
    console.error('❌ API connection test failed:', error);
    return {
      mainApi: false,
      idGenApi: false
    };
  }
};