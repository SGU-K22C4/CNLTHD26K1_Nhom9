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
public class OccasionAdvice {

    private String occasion;
    private String recommendedDirection;
    private String colorGuidance;
    private String closingAngle;

    @Builder.Default
    private List<String> avoid = new ArrayList<>();

    public static OccasionAdvice empty() {
        return OccasionAdvice.builder().build();
    }
}
