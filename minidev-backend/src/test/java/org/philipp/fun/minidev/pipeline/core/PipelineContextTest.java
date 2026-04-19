package org.philipp.fun.minidev.pipeline.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.philipp.fun.minidev.pipeline.impl.SequenzStage;
import org.philipp.fun.minidev.pipeline.impl.LambdaStep;

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
            context.putValue(STRING_KEY, value);
            String retrieved = context.getValue(STRING_KEY);

            // Assert
            assertThat(retrieved).isEqualTo(value);
        }

        @Test
        @DisplayName("contains returns true for existing key")
        void testContains() {
            // Arrange
            context.putValue(STRING_KEY, "exists");

            // Act
            boolean contains = context.containsValue(STRING_KEY);

            // Assert
            assertThat(contains).isTrue();
        }

        @Test
        @DisplayName("get returns null for non-existing key")
        void testGetNull() {
            // Act
            String retrieved = context.getValue(STRING_KEY);

            // Assert
            assertThat(retrieved).isNull();
        }

        @Test
        @DisplayName("returns null when retrieving with different type via same name key")
        void testWrongTypeReturnsNull() {
            // Arrange
            ContextKey<String> stringKey = new ContextKey<>("key", String.class);
            context.putValue(stringKey, "not an integer");

            // Act & Assert
            ContextKey<Integer> intKey = new ContextKey<>("key", Integer.class);
            assertThat(context.getValue(intKey)).isNull();
        }
    }

    @Nested
    @DisplayName("Key-based Context Access")
    class StringAccessTests {

        @ParameterizedTest
        @ValueSource(strings = {"key1", "another_key", "namespace.key"})
        @DisplayName("put and get key-based values")
        void testStringPutGet(String name) {
            // Arrange
            ContextKey<Object> key = new ContextKey<>(name, Object.class);
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
            PipelineElement pipeline = mock(PipelineElement.class);

            // Act
            context.setPipeline(pipeline);
            PipelineElement retrieved = context.getPipeline();

            // Assert
            assertThat(retrieved).isSameAs(pipeline);
        }
    }

    @Nested
    @DisplayName("Nested Stages should have the same Context")
    class NestedStageContextTests {
        @Test
        @DisplayName("nested stages share the same context")
        void testNestedStageContext() throws Exception {
            // Arrange
            SequenzStage stage = new SequenzStage("Test Pipeline");
            stage.addElement(new LambdaStep("Outer Step", ctx -> {
                ctx.putValue(new ContextKey<>("key", String.class), "value");
                return true;
            }));
            
            SequenzStage inner = new SequenzStage("Inner Stage");
            inner.addElement(new LambdaStep("Inner Step", ctx -> {
                String value = ctx.getValue(new ContextKey<>("key", String.class));
                assertThat(value).isEqualTo("value");
                return true;
            }));
            stage.addElement(inner);

            // Act
            boolean success = stage.execute(new PipelineContext());

            // Assert
            assertThat(success).isTrue();
        }
    }
}
