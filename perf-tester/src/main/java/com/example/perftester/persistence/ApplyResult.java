package com.example.perftester.persistence;

import java.util.List;

public record ApplyResult(List<String> applied, List<String> errors) {
}
