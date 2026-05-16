package com.fashion.chatbotservice.service;

import java.util.List;
import java.util.Set;

public interface ProductTaxonomyService {

    List<String> extractTypeLabels(String name, String category);

    String resolveGroupLabel(String value);

    String inferOccasionContext(String normalizedSearch);

    Set<String> inferTaxonomyLabels(String haystack);
}
