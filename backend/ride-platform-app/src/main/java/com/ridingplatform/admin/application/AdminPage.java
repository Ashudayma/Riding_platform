package com.ridingplatform.admin.application;

import java.util.List;

public record AdminPage<T>(
        List<T> items,
        long totalElements,
        int page,
        int size,
        int totalPages
) {
}
