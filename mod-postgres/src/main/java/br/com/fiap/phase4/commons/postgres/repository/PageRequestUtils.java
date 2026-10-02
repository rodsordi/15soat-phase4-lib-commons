package br.com.fiap.phase4.commons.postgres.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequestUtils {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PageRequestUtils() {
    }

    public static Pageable of(Integer page, Integer size, String sortBy, String direction) {
        int pageIndex = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int pageSize = (size != null && size > 0) ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String property = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim() : "createdAt";

        return PageRequest.of(pageIndex, pageSize, Sort.by(sortDirection, property));
    }
}
