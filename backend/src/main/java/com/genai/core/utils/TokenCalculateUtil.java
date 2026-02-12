package com.genai.core.utils;

import com.genai.core.config.properties.LlmProperty;
import com.genai.core.repository.vo.ConversationVO;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

import java.util.List;

public class TokenCalculateUtil {

    private static final Encoding encoding;

    static {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        encoding = registry.getEncoding(EncodingType.CL100K_BASE);
    }

    public static int calculateMaxTokens(LlmProperty llmProperty, String prompt, String query, String chatState, List<ConversationVO> conversations, String context) {

        int inputTokens = 0;

        inputTokens += count(prompt);
        inputTokens += count(query);
        inputTokens += count(chatState);
        inputTokens += count(context);

        if (conversations != null) {
            for (ConversationVO conversation : conversations) {
                inputTokens += count(conversation.getQuery());
                inputTokens += count(conversation.getAnswer());
            }
        }

        // 🔧 내부 토큰 보정
        inputTokens += llmProperty.getInternalTokenOverhead();

        int available =
                llmProperty.getModelContextLimit()
                        - inputTokens
                        - llmProperty.getSafetyMargin();

        return clamp(
                available,
                llmProperty.getMinOutputTokens(),
                llmProperty.getMaxOutputTokens()
        );
    }

    private static int count(String text) {
        return countTokens(text);
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    public static int countTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return encoding.countTokens(text);
    }
}
