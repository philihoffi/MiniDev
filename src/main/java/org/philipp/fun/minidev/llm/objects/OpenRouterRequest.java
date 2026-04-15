package org.philipp.fun.minidev.llm.objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenRouterRequest(
    List<Message> messages,
    String model,
    Double temperature,
    @JsonProperty("max_tokens") Integer maxTokens,
    @JsonProperty("response_format") ResponseFormat responseFormat,
    @JsonProperty("session_id") String sessionId
) {
    public record Message(
        String role,
        String content
    ) {}

    public record ResponseFormat(
        String type,
        @JsonProperty("json_schema") JsonSchema jsonSchema
    ) {
        public static ResponseFormat JSON = new ResponseFormat("json_object", null);
    }
}
