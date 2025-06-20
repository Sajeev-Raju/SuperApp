package com.example.NearMeBKND.polls.model;

import java.util.List;
import java.util.Map;

public class PaginatedResponse {
    private List<Map<String, Object>> content;
    private int page;
    private int size;
    private int totalElements;
    private int totalPages;
    private int currentPageSize;

    public PaginatedResponse(List<Map<String, Object>> content, int page, int size, int totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / size);
        this.currentPageSize = content.size();
    }

    public List<Map<String, Object>> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getCurrentPageSize() {
        return currentPageSize;
    }
} 