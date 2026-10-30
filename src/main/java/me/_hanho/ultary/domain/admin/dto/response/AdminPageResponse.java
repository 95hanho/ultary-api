package me._hanho.ultary.domain.admin.dto.response;

import java.util.List;

public record AdminPageResponse<T>(List<T> items, int totalCount, int page, int pageSize) {}
