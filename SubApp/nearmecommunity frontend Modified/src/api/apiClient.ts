import { user } from '../stores/userStore';
import { get } from 'svelte/store';

const API_BASE_URL = 'http://localhost:8080';

interface ApiOptions {
  method?: string;
  body?: any;
  headers?: Record<string, string>;
  isFormData?: boolean;
}

/**
 * Base API client function for making HTTP requests
 */
async function apiClient<T>(
  endpoint: string,
  { method = 'GET', body, headers = {}, isFormData = false }: ApiOptions = {}
): Promise<T> {
  const userId = get(user).userId;

  if (!userId) {
    throw new Error('User ID not found. Please log in again.');
  }

  const url = `${API_BASE_URL}${endpoint}`;
  const options: RequestInit = {
    method,
    headers: {
      'X-User-ID': userId,
      ...(!isFormData && { 'Content-Type': 'application/json' }),
      'Accept': 'application/json',
      ...headers,
    },
    credentials: 'include',
    mode: 'cors',
  };

  if (body) {
    options.body = isFormData ? body : JSON.stringify(body);
  }

  try {
    const response = await fetch(url, options);

    // Handle HTTP errors
    if (!response.ok) {
      let errorMessage = `API error: ${response.status}`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.error || errorData.message || errorMessage;
        console.error('API Error Details:', errorData);
      } catch (e) {
        // If response is not JSON, use status text
        errorMessage = response.statusText || errorMessage;
      }
      throw new Error(errorMessage);
    }

    // Check if response is JSON
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return await response.json();
    }

    return await response.text() as unknown as T;
  } catch (error) {
    if (error instanceof Error) {
      throw error;
    }
    throw new Error('Network error occurred. Please check your connection.');
  }
}

/**
 * Location API functions
 */
export const locationApi = {
  saveLocation: (data: { userId: string; latitude: number; longitude: number; locationName?: string }) => {
    return apiClient('/api/location/save', { method: 'POST', body: data });
  },
  
  getLocation: (userId: string) => {
    return apiClient(`/api/location/${userId}`);
  },
};

/**
 * Q&A API functions
 */
export const qandaApi = {
  getQuestions: (page = 0, size = 50) => {
    return apiClient(`/api/qanda/questions/?page=${page}&size=${size}`);
  },
  
  getQuestion: (id: string) => {
    return apiClient(`/api/qanda/questions/${id}`);
  },
  
  createQuestion: (data: { qTitle: string; questionDescription: string; tags: string[] }) => {
    return apiClient('/api/qanda/questions', { method: 'POST', body: data });
  },
  
  postAnswer: (data: { questionId: string; content: string }) => {
    return apiClient(`/api/qanda/questions/${data.questionId}/answers`, { method: 'POST', body: { description: data.content } });
  },
  
  addNote: (questionId: string, data: { content: string }) => {
    return apiClient(`/api/qanda/questions/${questionId}/notes`, { method: 'POST', body: data });
  },
  
  getTags: () => {
    return apiClient('/api/qanda/tags');
  },
};

/**
 * Meetups API functions
 */
export const meetupsApi = {
  getMeetups: (page = 0, size = 30) => {
    return apiClient(`/api/meetups/?page=${page}&size=${size}`);
  },
  
  getMeetup: (id: string) => {
    return apiClient(`/api/meetups/${id}`);
  },
  
  createMeetup: (formData: FormData) => {
    return apiClient('/api/meetups', { method: 'POST', body: formData, isFormData: true });
  },
  
  getUserMeetups: () => {
    return apiClient('/api/meetups/MyEvents');
  },
  
  updateMeetupNote: (id: string, data: { note: string }) => {
    return apiClient(`/api/meetups/${id}/note`, { method: 'PATCH', body: data });
  },
  
  deleteMeetup: (id: string) => {
    return apiClient(`/api/meetups/${id}`, { method: 'DELETE' });
  },
  
  searchMeetups: (tags: string[]) => {
    const queryParams = new URLSearchParams();
    tags.forEach(tag => queryParams.append('tags', tag));
    return apiClient(`/api/meetups/search?${queryParams.toString()}`);
  },
  
  getMeetupImageUrl: (id: string) => {
    return `${API_BASE_URL}/api/meetups/${id}/image`;
  },
  
  postMeetupQuestion: (id: string, data: { content: string }) => {
    return apiClient(`/api/meetups/${id}/questions`, { method: 'POST', body: data });
  },

  getMeetupQuestions: (id: string) => {
    return apiClient(`/api/meetups/${id}/questions`);
  },
};

/**
 * Classifieds API functions
 */
export const classifiedsApi = {
  getClassifieds: (page = 0, size = 30) => {
    return apiClient(`/api/classified/?page=${page}&size=${size}`);
  },
  
  getClassified: (id: string) => {
    return apiClient(`/api/classified/${id}`);
  },
  
  createClassified: (formData: FormData) => {
    return apiClient('/api/classified', { method: 'POST', body: formData, isFormData: true });
  },
  
  getCategories: () => {
    return apiClient('/api/classified/categories');
  },
  
  deleteClassified: (id: string) => {
    return apiClient(`/api/classified/${id}`, { method: 'DELETE' });
  },
  
  addNote: (id: string, data: { note: string }) => {
    return apiClient(`/api/classified/${id}/notes`, { method: 'POST', body: data });
  },
  
  askQuestion: (classifiedId: number, data: { content: string }) => {
    return apiClient(`/api/classified/question/${classifiedId}`, { method: 'POST', body: data });
  },
};

/**
 * Polls API functions
 */
export interface PollQuestion {
  questionId: number;
  userId: string;
  questionText: string;
  selectionLimit: number;
  selectionMode: string;
  latitude: number;
  longitude: number;
  options: PollOption[];
}

export interface PollOption {
  optionId: number;
  questionId: number;
  optionText: string;
  voteCount: number;
}

export interface VotedPoll {
  questionId: number;
  questionText: string;
  votedOptions: string[];
}

export const pollsApi = {
  getPolls: (page = 0, size = 30) => {
    return apiClient<PollQuestion[]>(`/api/polls/questions/?page=${page}&size=${size}`);
  },
  
  createPoll: (data: { 
    questionText: string; 
    pollOptions: string[]; 
    selectionLimit: number;
    selectionMode: string;
  }) => {
    return apiClient('/api/polls/questions', { method: 'POST', body: data });
  },

  createMultiplePolls: (data: { 
    questionText: string; 
    pollOptions: string[]; 
    selectionLimit: number;
    selectionMode: string;
  }[]) => {
    return apiClient('/api/polls/questions', { method: 'POST', body: data });
  },
  
  getPollResults: (id: string) => {
    return apiClient<PollOption[]>(`/api/polls/questions/${id}/results`);
  },
  
  vote: (id: string, data: { optionIds: number[] }) => {
    return apiClient(`/api/polls/questions/${id}/vote`, { method: 'POST', body: data });
  },
  
  getVotedPolls: () => {
    return apiClient<VotedPoll[]>('/api/polls/questions/voted');
  },
  
  deletePoll: (id: string) => {
    return apiClient(`/api/polls/questions/${id}`, { method: 'DELETE' });
  },
};

/**
 * Business API functions
 */
export interface Business {
  businessId: number;
  userId: string;
  name: string;
  title: string;
  tags: string;
  description: string;
  address: string;
  mobileNumber: string;
  timings: string;
  googlemapsURL: string;
  image: string;
  createdAt: string;
  active: boolean;
}

export const businessApi = {
  getBusinesses: (page = 0, size = 30) => {
    return apiClient(`/api/business/?page=${page}&size=${size}`);
  },
  
  getBusiness: (id: string) => {
    return apiClient(`/api/business/${id}`);
  },
  
  createBusiness: (formData: FormData) => {
    return apiClient('/api/business/', { method: 'POST', body: formData, isFormData: true });
  },
  
  getMyBusinesses: () => {
    return apiClient('/api/business/mine');
  },
  
  deleteBusiness: (id: string) => {
    return apiClient(`/api/business/${id}`, { method: 'DELETE' });
  },
  
  getBusinessImageUrl: (id: string) => {
    return `${API_BASE_URL}/api/business/image/${id}`;
  },
  
  postQuestion: (data: { businessId: number; questionText: string }) => {
    return apiClient('/api/business/question', { method: 'POST', body: data });
  },
  
  postAnswer: (data: { questionId: number; answerText: string }) => {
    return apiClient('/api/business/answer', { method: 'POST', body: data });
  },
  
  getMyQuestionsWithAnswers: () => {
    return apiClient('/api/business/questions/mine');
  },
  
  postFeedback: (businessId: number, data: { feedbackText: string }) => {
    return apiClient(`/api/business/${businessId}/feedback`, { method: 'POST', body: data });
  },
  
  getFeedback: (businessId: number) => {
    return apiClient(`/api/business/${businessId}/feedback`);
  },
};

export interface EmergencyMessage {
  id: number;
  title: string;
  description: string;
  types: string[];
  details: Record<string, string>;
  googleMapsLocation: string | null;
  userId: string;
  createdAt: string;
  latitude: number;
  longitude: number;
  notes: string[];
}

/**
 * Emergency Messages API functions
 */
export const emergencyApi = {
  getMessages: async (): Promise<EmergencyMessage[]> => {
    const result = await apiClient<{ data: EmergencyMessage[] }>('/api/emergency_messages/');
    return Array.isArray(result.data) ? result.data : [];
  },
  
  createMessage: (data: { 
    title: string; 
    description: string; 
    types: string[];
    details: Record<string, string>; 
    googleMapsLocation: string | null;
  }): Promise<{ message: string; created_at: string }> => {
    return apiClient('/api/emergency_messages', { method: 'POST', body: data });
  },
  
  deleteMessage: (id: string): Promise<{ message: string }> => {
    return apiClient(`/api/emergency_messages/${id}`, { method: 'DELETE' });
  },
  
  askQuestion: (id: string, data: { content: string }): Promise<{ id: number }> => {
    return apiClient(`/api/emergency_messages/${id}/question`, { method: 'POST', body: data });
  },
  
  addNote: (id: string, data: { content: string }): Promise<{ message: string; emergencyId: number }> => {
    return apiClient(`/api/emergency_messages/${id}/notes`, { method: 'POST', body: { note: data.content } });
  }
};

export default {
  location: locationApi,
  qanda: qandaApi,
  meetups: meetupsApi,
  classifieds: classifiedsApi,
  polls: pollsApi,
  business: businessApi,
  emergency: emergencyApi,
};