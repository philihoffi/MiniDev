package org.philipp.fun.minidev.pipeline.composite;

import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import java.util.Objects;
import java.util.function.Predicate;

public class Conditional extends BaseElement {
    private final Predicate<PipelineContext> condition;
    private final PipelineElement thenBranch;
    private final PipelineElement elseBranch;

    public Conditional(String name, Predicate<PipelineContext> condition, PipelineElement thenBranch, PipelineElement elseBranch) {
        super(name);
        this.condition = Objects.requireNonNull(condition, "condition must not be null");
        this.thenBranch = Objects.requireNonNull(thenBranch, "thenBranch must not be null");
        this.elseBranch = elseBranch;
    }

    public Conditional(String name, Predicate<PipelineContext> condition, PipelineElement thenBranch) {
        this(name, condition, thenBranch, null);
    }

    @Override
    public boolean execute(PipelineContext ctx) {
        boolean result = condition.test(ctx);
        PipelineElement branch = result ? thenBranch : elseBranch;
        if (branch == null) {
            return result;
        }
        return runElement(branch, ctx);
    }

    public Predicate<PipelineContext> getCondition() { return condition; }
    public PipelineElement getThenBranch() { return thenBranch; }
    public PipelineElement getElseBranch() { return elseBranch; }
}
