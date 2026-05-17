package com.fashion.chatbotservice.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StylingSlots {

    private String targetGender;
    private String occasion;
    private String styleVibe;
    private String productType;
    private String budget;
    private String size;
    private String fitPreference;
    private String colorPreference;
    private Integer heightCm;
    private Integer weightKg;

    public static StylingSlots empty() {
        return StylingSlots.builder().build();
    }
}
