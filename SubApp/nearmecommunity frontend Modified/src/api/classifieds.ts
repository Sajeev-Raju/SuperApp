import type { 
  Classified, 
  ClassifiedFilters, 
  CreateClassifiedData, 
  QuestionData, 
  NoteData 
} from "../types/classified";
import { api } from "./api";

export const classifieds = {
  async getClassifieds(filters?: ClassifiedFilters): Promise<Classified[]> {
    try {
      const params = new URLSearchParams();
      
      // Search filter
      if (filters?.search) {
        params.append("search", filters.search);
      }
      
      // Categories filter
      if (filters?.categories?.length) {
        params.append("categories", filters.categories.join(","));
      }
      
      // Sort filter
      if (filters?.sortBy) {
        params.append("sortBy", filters.sortBy);
      }
      
      // Price range filters
      if (filters?.priceMin !== undefined) {
        params.append("priceMin", filters.priceMin.toString());
      }
      if (filters?.priceMax !== undefined) {
        params.append("priceMax", filters.priceMax.toString());
      }

      const response = await api.get<Classified[]>(`/classifieds?${params.toString()}`);
      // Ensure we return an array
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error("Error fetching classifieds:", error);
      return [];
    }
  },

  async getClassified(id: string): Promise<Classified | null> {
    try {
      const response = await api.get<Classified>(`/classifieds/${id}`);
      return response.data || null;
    } catch (error) {
      console.error("Error fetching classified:", error);
      return null;
    }
  },

  async createClassified(formData: FormData): Promise<Classified | null> {
    try {
      const response = await api.post<Classified>("/classifieds", formData, {
        headers: {
          "Content-Type": "multipart/form-data"
        }
      });
      return response.data || null;
    } catch (error) {
      console.error("Error creating classified:", error);
      return null;
    }
  },

  async deleteClassified(id: string): Promise<boolean> {
    try {
      await api.delete(`/classifieds/${id}`);
      return true;
    } catch (error) {
      console.error("Error deleting classified:", error);
      return false;
    }
  },

  async getCategories(): Promise<string[]> {
    try {
      const response = await api.get<string[]>("/classifieds/categories");
      // Ensure we return an array
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error("Error fetching categories:", error);
      return [];
    }
  },

  async askQuestion(classifiedId: string, data: QuestionData): Promise<boolean> {
    try {
      await api.post(`/classifieds/${classifiedId}/questions`, data);
      return true;
    } catch (error) {
      console.error("Error asking question:", error);
      return false;
    }
  },

  async addNote(classifiedId: string, data: NoteData): Promise<boolean> {
    try {
      await api.post(`/classifieds/${classifiedId}/notes`, data);
      return true;
    } catch (error) {
      console.error("Error adding note:", error);
      return false;
    }
  }
}; 