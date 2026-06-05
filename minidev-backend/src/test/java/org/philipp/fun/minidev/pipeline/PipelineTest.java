package org.philipp.fun.minidev.pipeline;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

    @Test
    void testConditionalExecutesThenBranch() throws Exception {
        ContextKey<Boolean> flag = new ContextKey<>("flag", Boolean.class);
        PipelineContext ctx = new PipelineContext();
        ctx.putValue(flag, true);

        StringBuilder sb = new StringBuilder();
        Conditional cond = new Conditional("TestCond",
                c -> Boolean.TRUE.equals(c.getValue(flag)),
                new BaseElement("ThenStep") {
                    @Override public boolean execute(PipelineContext c) { sb.append("then"); return true; }
                },
                new BaseElement("ElseStep") {
                    @Override public boolean execute(PipelineContext c) { sb.append("else"); return true; }
                });

        boolean success = cond.execute(ctx);
        assertThat(success).isTrue();
        assertThat(sb.toString()).isEqualTo("then");
    }

    @Test
    void testConditionalExecutesElseBranch() throws Exception {
        ContextKey<Boolean> flag = new ContextKey<>("flag", Boolean.class);
        PipelineContext ctx = new PipelineContext();
        ctx.putValue(flag, false);

        StringBuilder sb = new StringBuilder();
        Conditional cond = new Conditional("TestCond",
                c -> Boolean.TRUE.equals(c.getValue(flag)),
                new BaseElement("ThenStep") {
                    @Override public boolean execute(PipelineContext c) { sb.append("then"); return true; }
                },
                new BaseElement("ElseStep") {
                    @Override public boolean execute(PipelineContext c) { sb.append("else"); return true; }
                });

        boolean success = cond.execute(ctx);
        assertThat(success).isTrue();
        assertThat(sb.toString()).isEqualTo("else");
    }

    @Test
    void testConditionalWithoutElseBranch() throws Exception {
        ContextKey<Boolean> flag = new ContextKey<>("flag", Boolean.class);
        PipelineContext ctx = new PipelineContext();
        ctx.putValue(flag, false);

        Conditional cond = new Conditional("TestCond",
                c -> Boolean.TRUE.equals(c.getValue(flag)),
                new BaseElement("ThenStep") {
                    @Override public boolean execute(PipelineContext c) { return true; }
                });

        boolean success = cond.execute(ctx);
        assertThat(success).isFalse();
    }

    @Test
    void testConditionalReturnsFalseOnBranchFailure() throws Exception {
        ContextKey<Boolean> flag = new ContextKey<>("flag", Boolean.class);
        PipelineContext ctx = new PipelineContext();
        ctx.putValue(flag, true);

        Conditional cond = new Conditional("TestCond",
                c -> Boolean.TRUE.equals(c.getValue(flag)),
                new BaseElement("FailStep") {
                    @Override public boolean execute(PipelineContext c) { return false; }
                });

        boolean success = cond.execute(ctx);
        assertThat(success).isFalse();
    }

    @Test
    void testConditionalInSequence() throws Exception {
        ContextKey<Boolean> flag = new ContextKey<>("flag", Boolean.class);
        PipelineContext ctx = new PipelineContext();
        ctx.putValue(flag, true);

        StringBuilder sb = new StringBuilder();
        Sequence seq = new Sequence("TestSeq");
        seq.add(new BaseElement("Before") {
            @Override public boolean execute(PipelineContext c) { sb.append("1"); return true; }
        }).add(new Conditional("Branch",
                c -> Boolean.TRUE.equals(c.getValue(flag)),
                new BaseElement("ThenStep") {
                    @Override public boolean execute(PipelineContext c) { sb.append("T"); return true; }
                },
                new BaseElement("ElseStep") {
                    @Override public boolean execute(PipelineContext c) { sb.append("E"); return true; }
                })
        ).add(new BaseElement("After") {
            @Override public boolean execute(PipelineContext c) { sb.append("2"); return true; }
        });

        boolean success = seq.execute(ctx);
        assertThat(success).isTrue();
        assertThat(sb.toString()).isEqualTo("1T2");
    }

    @Test
    void testConditionalNotifiesListeners() throws Exception {
        List<String> events = new ArrayList<>();
        ContextKey<Boolean> flag = new ContextKey<>("flag", Boolean.class);
        PipelineContext ctx = new PipelineContext();
        ctx.putValue(flag, true);

        Conditional cond = new Conditional("TestCond",
                c -> Boolean.TRUE.equals(c.getValue(flag)),
                new BaseElement("ThenStep") {
                    @Override public boolean execute(PipelineContext c) { return true; }
                });
        cond.setListeners(List.of(new PipelineListener() {
            @Override public void onStart(PipelineElement e, PipelineContext c) { events.add("start:" + e.getName()); }
            @Override public void onEnd(PipelineElement e, PipelineContext c, boolean r) { events.add("end:" + e.getName()); }
        }));

        cond.execute(ctx);
        assertThat(events).containsExactly("start:ThenStep", "end:ThenStep");
    }
}
