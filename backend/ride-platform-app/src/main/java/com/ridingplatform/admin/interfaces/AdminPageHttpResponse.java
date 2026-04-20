package com.ridingplatform.admin.interfaces;

import java.util.List;

public record AdminPageHttpResponse<T>(
        List<T> items,
        long totalElements,
        int page,
        int size,
        int totalPages
) {
}
