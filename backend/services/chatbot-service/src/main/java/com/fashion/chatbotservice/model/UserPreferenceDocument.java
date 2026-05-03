package com.fashion.chatbotservice.model;

import com.fashion.chatbotservice.model.ChatSession.PreferenceProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Persistent user preference profile stored in MongoDB.
 * Survives across browser sessions — allows AI to remember user's size, colors, style, and budget.
 * Separate from ChatSession to avoid coupling profile lifecycle with chat lifecycle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_preferences")
public class UserPreferenceDocument {

    @Id
    private String id;

    /** Unique user identifier — matches X-User-Id header from Kong Gateway */
    @Indexed(unique = true)
    private String userId;

    /** The full preference profile extracted from chat interactions and purchase history */
    private PreferenceProfile profile;

    /** Last time this profile was updated (for staleness detection) */
    private Instant lastUpdatedAt;
}
