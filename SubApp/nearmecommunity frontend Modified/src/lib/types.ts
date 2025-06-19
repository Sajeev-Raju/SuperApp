export interface Business {
  id: number;
  name: string;
  title: string;
  description: string;
  address: string;
  image_url?: string;
  tags: string[];
  location: {
    latitude: number;
    longitude: number;
  };
  owner_id: number;
  questions: Array<{
    id: number;
    question: string;
    answer?: string;
    created_at: string;
  }>;
  feedback: Array<{
    id: number;
    content: string;
    reply?: string;
    created_at: string;
  }>;
  notifications: Array<{
    id: number;
    content: string;
    created_at: string;
  }>;
  created_at: string;
  updated_at: string;
} 