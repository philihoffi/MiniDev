package org.philipp.fun.minidev.llm.dto;

/**
 * Data record representing a JSON schema configuration for structured LLM
 * output.
 *
 * @param name   the schema name
 * @param strict whether strict schema validation is enabled
 * @param schema the actual JSON schema object
 */
public record JsonSchema(
        String name,
        Boolean strict,
        Object schema
) {

    /**
     * Creates a default schema with strict mode enabled.
     *
     * @param schema the JSON schema object
     * @return a new {@code JsonSchema} with defaults
     */
    public static JsonSchema defaultSchema(Object schema) {
        return new JsonSchema("structured_output", true, schema);
    }
}