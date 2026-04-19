package org.philipp.fun.minidev.llm.objects;

public record JsonSchema(
        String name,
        Boolean strict,
        Object schema
) {
    public static JsonSchema defaultSchema(Object schema) {
        return new JsonSchema("structured_output", true, schema);
    }
}
