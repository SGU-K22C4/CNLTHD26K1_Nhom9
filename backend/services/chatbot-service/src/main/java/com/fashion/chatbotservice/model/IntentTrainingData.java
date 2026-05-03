package com.fashion.chatbotservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "intents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentTrainingData {

    @Id
    private String id;

    private String intentName;

    @Builder.Default
    private List<String> examples = new ArrayList<>();

    private String responseTemplate;

    private Instant createdAt;
}
