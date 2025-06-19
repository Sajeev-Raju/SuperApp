export interface Classified {
  id: number;
  title: string;
  description: string;
  price: number;
  imageUrlString: string;
  categories: string[];
  userId: string;
  createdAt: string;
}

export interface ClassifiedFilters {
  search?: string;
  categories?: string[];
  sortBy?: "newest" | "oldest" | "price-low" | "price-high";
  priceMin?: number;
  priceMax?: number;
}

export interface CreateClassifiedData {
  title: string;
  description: string;
  price: number;
  categories: string[];
  image?: File;
}

export interface QuestionData {
  question: string;
}

export interface NoteData {
  note: string;
} 