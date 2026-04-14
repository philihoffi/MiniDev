package org.philipp.fun.minidev.pipeline.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("Pipeline Context and Keys Tests")
class PipelineContextTest {

    private PipelineContext context;

    @BeforeEach
    void setUp() {
        context = new PipelineContext();
    }

    @Nested
    @DisplayName("Typed Context Access")
    class TypedAccessTests {
        
        private final ContextKey<String> STRING_KEY = new ContextKey<>("test.string", String.class);
        private final ContextKey<Integer> INT_KEY = new ContextKey<>("test.int", Integer.class);

        @Test
        @DisplayName("put and get typed value")
        void testTypedPutGet() {
            // Arrange
            String value = "hello";

            // Act
            context.put(STRING_KEY, value);
            String retrieved = context.get(STRING_KEY);

            // Assert
            assertThat(retrieved).isEqualTo(value);
        }

        @Test
        @DisplayName("contains returns true for existing key")
        void testContains() {
            // Arrange
            context.put(STRING_KEY, "exists");

            // Act
            boolean contains = context.contains(STRING_KEY);

            // Assert
            assertThat(contains).isTrue();
        }

        @Test
        @DisplayName("get returns null for non-existing key")
        void testGetNull() {
            // Act
            String retrieved = context.get(STRING_KEY);

            // Assert
            assertThat(retrieved).isNull();
        }

        @Test
        @DisplayName("throws ClassCastException when retrieving with wrong type via string key")
        void testWrongType() {
            // Arrange
            context.put("key", "not an integer");

            // Act & Assert
            assertThatThrownBy(() -> context.get("key", Integer.class))
                    .isInstanceOf(ClassCastException.class);
        }
    }

    @Nested
    @DisplayName("String-based Context Access")
    class StringAccessTests {

        @ParameterizedTest
        @ValueSource(strings = {"key1", "another_key", "namespace.key"})
        @DisplayName("put and get string-based values")
        void testStringPutGet(String key) {
            // Arrange
            Object value = new Object();

            // Act
            context.put(key, value);
            Object retrieved = context.get(key);

            // Assert
            assertThat(retrieved).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("Pipeline Reference")
    class PipelineReferenceTests {
        @Test
        @DisplayName("set and get pipeline reference")
        void testPipelineRef() {
            // Arrange
            Pipeline pipeline = mock(Pipeline.class);

            // Act
            context.setPipeline(pipeline);
            Pipeline retrieved = context.getPipeline();

            // Assert
            assertThat(retrieved).isSameAs(pipeline);
        }
    }
}
