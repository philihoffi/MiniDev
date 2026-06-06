package org.philipp.fun.minidev.pipeline;

import org.junit.jupiter.api.Test;
import org.philipp.fun.minidev.pipeline.composite.CircuitBreaker;
import org.philipp.fun.minidev.pipeline.composite.Conditional;
import org.philipp.fun.minidev.pipeline.composite.ForkJoin;
import org.philipp.fun.minidev.pipeline.composite.Parallel;
import org.philipp.fun.minidev.pipeline.composite.Retry;
import org.philipp.fun.minidev.pipeline.composite.Sequence;
import org.philipp.fun.minidev.pipeline.composite.Switch;
import org.philipp.fun.minidev.pipeline.composite.Timeout;
import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.ContextKey;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.philipp.fun.minidev.pipeline.core.PipelineListener;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    void testParallelExecutesAllChildren() throws Exception {
        Parallel parallel = new Parallel("TestParallel");
        StringBuilder sb = new StringBuilder();

        parallel.add(new BaseElement("A") {
            @Override public boolean execute(PipelineContext ctx) throws Exception {
                Thread.sleep(10); sb.append("A"); return true;
            }
        }).add(new BaseElement("B") {
            @Override public boolean execute(PipelineContext ctx) throws Exception {
                Thread.sleep(5); sb.append("B"); return true;
            }
        }).add(new BaseElement("C") {
            @Override public boolean execute(PipelineContext ctx) throws Exception {
                sb.append("C"); return true;
            }
        });

        boolean ok = parallel.execute(new PipelineContext());
        assertThat(ok).isTrue();
        assertThat(sb.toString()).contains("A", "B", "C");
        assertThat(sb).hasToString("CBA");
    }

    @Test
    void testParallelReturnsFalseOnChildFailure() throws Exception {
        Parallel parallel = new Parallel("TestParallel");

        parallel.add(new BaseElement("Good") {
            @Override public boolean execute(PipelineContext ctx) { return true; }
        }).add(new BaseElement("Bad") {
            @Override public boolean execute(PipelineContext ctx) { return false; }
        });

        boolean ok = parallel.execute(new PipelineContext());
        assertThat(ok).isFalse();
    }

    @Test
    void testForkJoinExecutesForksAndJoin() throws Exception {
        StringBuilder sb = new StringBuilder();
        PipelineElement join = new BaseElement("Join") {
            @Override public boolean execute(PipelineContext ctx) { sb.append("J"); return true; }
        };
        ForkJoin fj = new ForkJoin("TestFJ", join);

        fj.fork(new BaseElement("F1") {
            @Override public boolean execute(PipelineContext ctx) { sb.append("1"); return true; }
        }).fork(new BaseElement("F2") {
            @Override public boolean execute(PipelineContext ctx) { sb.append("2"); return true; }
        });

        boolean ok = fj.execute(new PipelineContext());
        assertThat(ok).isTrue();
        assertThat(sb.toString()).contains("1", "2", "J");
    }

    @Test
    void testSwitchExecutesMatchingCase() throws Exception {
        ContextKey<String> key = new ContextKey<>("mode", String.class);
        PipelineContext ctx = new PipelineContext();
        ctx.putValue(key, "fast");

        StringBuilder sb = new StringBuilder();
        Switch sw = new Switch("TestSwitch");
        sw.addCase(c -> "fast".equals(c.getValue(key)),
                new BaseElement("Fast") {
                    @Override public boolean execute(PipelineContext c) { sb.append("fast"); return true; }
                });
        sw.addCase(c -> "slow".equals(c.getValue(key)),
                new BaseElement("Slow") {
                    @Override public boolean execute(PipelineContext c) { sb.append("slow"); return true; }
                });

        boolean ok = sw.execute(ctx);
        assertThat(ok).isTrue();
        assertThat(sb.toString()).isEqualTo("fast");
    }

    @Test
    void testSwitchFallsThroughToDefault() throws Exception {
        ContextKey<String> key = new ContextKey<>("mode", String.class);
        PipelineContext ctx = new PipelineContext();
        ctx.putValue(key, "unknown");

        StringBuilder sb = new StringBuilder();
        Switch sw = new Switch("TestSwitch");
        sw.addCase(c -> "fast".equals(c.getValue(key)),
                new BaseElement("Fast") {
                    @Override public boolean execute(PipelineContext c) { sb.append("fast"); return true; }
                });
        sw.defaultBranch(new BaseElement("Default") {
            @Override public boolean execute(PipelineContext c) { sb.append("default"); return true; }
        });

        boolean ok = sw.execute(ctx);
        assertThat(ok).isTrue();
        assertThat(sb.toString()).isEqualTo("default");
    }

    @Test
    void testTimeoutCompletesWithinLimit() throws Exception {
        Timeout timeout = new Timeout("TestTimeout",
                new BaseElement("Fast") {
                    @Override public boolean execute(PipelineContext ctx) { return true; }
                },
                Duration.ofSeconds(5));

        boolean ok = timeout.execute(new PipelineContext());
        assertThat(ok).isTrue();
    }

    @Test
    void testTimeoutTriggersOnSlowElement() throws Exception {
        Timeout timeout = new Timeout("TestTimeout",
                new BaseElement("Slow") {
                    @Override public boolean execute(PipelineContext ctx) throws Exception {
                        Thread.sleep(500);
                        return true;
                    }
                },
                Duration.ofMillis(50));

        boolean ok = timeout.execute(new PipelineContext());
        assertThat(ok).isFalse();
    }

    @Test
    void testCircuitBreakerClosesAfterReset() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        CircuitBreaker cb = new CircuitBreaker("TestCB",
                new BaseElement("FailTwice") {
                    @Override public boolean execute(PipelineContext ctx) {
                        return count.incrementAndGet() > 2;
                    }
                },
                2, Duration.ofMillis(50));

        PipelineContext ctx = new PipelineContext();
        assertThat(cb.execute(ctx)).isFalse();
        assertThat(cb.execute(ctx)).isFalse();
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.CircuitState.OPEN);
        Thread.sleep(60);
        assertThat(cb.execute(ctx)).isTrue();
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.CircuitState.CLOSED);
    }

    @Test
    void testCircuitBreakerSkipsWhenOpen() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);
        CircuitBreaker cb = new CircuitBreaker("TestCB",
                new BaseElement("AlwaysFail") {
                    @Override public boolean execute(PipelineContext ctx) {
                        calls.incrementAndGet();
                        return false;
                    }
                },
                1, Duration.ofSeconds(60));

        PipelineContext ctx = new PipelineContext();
        assertThat(cb.execute(ctx)).isFalse();
        assertThat(cb.execute(ctx)).isFalse();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void testParallelEmptyReturnsTrue() throws Exception {
        Parallel p = new Parallel("Empty");
        assertThat(p.execute(new PipelineContext())).isTrue();
    }

    @Test
    void testSwitchNoMatchAndNoDefaultReturnsTrue() throws Exception {
        Switch sw = new Switch("Empty");
        boolean ok = sw.execute(new PipelineContext());
        assertThat(ok).isTrue();
    }

    @Test
    void testForkJoinFailsOnForkFailure() throws Exception {
        PipelineElement join = new BaseElement("Join") {
            @Override public boolean execute(PipelineContext ctx) { return true; }
        };
        ForkJoin fj = new ForkJoin("TestFJ", join);
        fj.fork(new BaseElement("Fails") {
            @Override public boolean execute(PipelineContext ctx) { return false; }
        });

        boolean ok = fj.execute(new PipelineContext());
        assertThat(ok).isFalse();
    }
}
