package com.example.perftester.rest;

import java.util.List;

public record BulkDeleteRequest(List<Long> ids) {
}
