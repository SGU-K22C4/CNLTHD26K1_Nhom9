package com.fashion.chatbotservice.styling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BodyShapeAdvice {

    private String recommendedFit;

    @Builder.Default
    private List<String> avoid = new ArrayList<>();

    @Builder.Default
    private List<String> stylingTips = new ArrayList<>();

    private double confidence;

    public static BodyShapeAdvice empty() {
        return BodyShapeAdvice.builder().build();
    }
}
