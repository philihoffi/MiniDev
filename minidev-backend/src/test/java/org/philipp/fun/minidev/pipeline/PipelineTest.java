package org.philipp.fun.minidev.pipeline;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PipelineTest {

    @Test
    void testSequenceExecution() throws Exception {
        Sequence seq = new Sequence("TestSeq");
        StringBuilder sb = new StringBuilder();

        seq.add(new BaseElement("S1") {
            @Override public boolean execute(PipelineContext ctx) { sb.append("1"); return true; }
        }).add(new BaseElement("S2") {
            @Override public boolean execute(PipelineContext ctx) { sb.append("2"); return true; }
        });

        boolean success = seq.execute(new PipelineContext());
        assertThat(success).isTrue();
        assertThat(sb.toString()).isEqualTo("12");
    }

    @Test
    void testSequenceStopsOnFailure() throws Exception {
        Sequence seq = new Sequence("TestSeq");
        StringBuilder sb = new StringBuilder();

        seq.add(new BaseElement("S1") {
            @Override public boolean execute(PipelineContext ctx) { sb.append("1"); return true; }
        }).add(new BaseElement("S2") {
            @Override public boolean execute(PipelineContext ctx) { sb.append("2"); return false; }
        }).add(new BaseElement("S3") {
            @Override public boolean execute(PipelineContext ctx) { sb.append("3"); return true; }
        });

        boolean success = seq.execute(new PipelineContext());
        assertThat(success).isFalse();
        assertThat(sb.toString()).isEqualTo("12");
    }

    @Test
    void testSequenceNullElementThrows() {
        Sequence seq = new Sequence("TestSeq");
        assertThatThrownBy(() -> seq.add(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void testRetryExecution() throws Exception {
        Retry retry = new Retry("TestRetry", 2);
        final int[] attempts = {0};

        retry.add(new BaseElement("Fail") {
            @Override public boolean execute(PipelineContext ctx) {
                attempts[0]++;
                return attempts[0] == 3;
            }
        });

        boolean success = retry.execute(new PipelineContext());
        assertThat(success).isTrue();
        assertThat(attempts[0]).isEqualTo(3);
    }

    @Test
    void testRetryExhaustsAttempts() throws Exception {
        Retry retry = new Retry("TestRetry", 2);
        final int[] attempts = {0};

        retry.add(new BaseElement("AlwaysFail") {
            @Override public boolean execute(PipelineContext ctx) {
                attempts[0]++;
                return false;
            }
        });

        boolean success = retry.execute(new PipelineContext());
        assertThat(success).isFalse();
        assertThat(attempts[0]).isEqualTo(3);
    }

    @Test
    void testRetryNotifiesWarning() throws Exception {
        Retry retry = new Retry("TestRetry", 3);
        List<String> warnings = new ArrayList<>();

        retry.setListeners(List.of(new PipelineListener() {
            @Override
            public void onWarning(PipelineElement element, PipelineContext context, String message) {
                warnings.add(message);
            }
        }));

        retry.add(new BaseElement("AlwaysFail") {
            @Override public boolean execute(PipelineContext ctx) { return false; }
        });

        retry.execute(new PipelineContext());
        assertThat(warnings).hasSize(3);
        assertThat(warnings.get(0)).contains("TestRetry");
    }

    @Test
    void testContextPutAndGet() {
        PipelineContext ctx = new PipelineContext();
        ContextKey<String> key = new ContextKey<>("test", String.class);

        ctx.putValue(key, "hello");
        assertThat(ctx.getValue(key)).isEqualTo("hello");
    }

    @Test
    void testContextMissingKeyReturnsNull() {
        PipelineContext ctx = new PipelineContext();
        ContextKey<String> key = new ContextKey<>("missing", String.class);

        assertThat(ctx.getValue(key)).isNull();
    }
}
