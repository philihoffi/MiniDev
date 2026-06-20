package org.philipp.fun.minidev.pipeline.composite;

import java.util.Objects;
import java.util.function.Predicate;

import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;

/**
 * A pipeline composite that conditionally executes one of two branches
 * based on a {@link Predicate}.
 */
public class Conditional extends BaseElement {

    /** The condition predicate. */
    private final Predicate<PipelineContext> condition;

    /** The branch executed when the condition evaluates to {@code true}. */
    private final PipelineElement thenBranch;

    /** The branch executed when the condition evaluates to {@code false}. */
    private final PipelineElement elseBranch;

    /**
     * Constructs a Conditional element with both branches.
     *
     * @param name       the element name
     * @param condition  the condition predicate
     * @param thenBranch the branch for {@code true}
     * @param elseBranch the branch for {@code false} (may be null)
     */
    public Conditional(
            String name,
            Predicate<PipelineContext> condition,
            PipelineElement thenBranch,
            PipelineElement elseBranch) {
        super(name);
        this.condition = Objects.requireNonNull(condition, "condition must not be null");
        this.thenBranch = Objects.requireNonNull(thenBranch, "thenBranch must not be null");
        this.elseBranch = elseBranch;
    }

    /**
     * Constructs a Conditional element with only a then-branch.
     *
     * @param name       the element name
     * @param condition  the condition predicate
     * @param thenBranch the branch for {@code true}
     */
    public Conditional(String name, Predicate<PipelineContext> condition, PipelineElement thenBranch) {
        this(name, condition, thenBranch, null);
    }

    /**
     * Executes the condition and dispatches to the appropriate branch.
     *
     * @param ctx the pipeline context
     * @return the result of the executed branch, or the condition result if no
     *         branch is configured
     */
    @Override
    public boolean execute(PipelineContext ctx) {
        boolean result = condition.test(ctx);
        PipelineElement branch = result ? thenBranch : elseBranch;
        if (branch == null) {
            return result;
        }
        return runElement(branch, ctx);
    }

    /**
     * Returns the condition predicate.
     *
     * @return the condition
     */
    public Predicate<PipelineContext> getCondition() {
        return condition;
    }

    /**
     * Returns the then-branch.
     *
     * @return the then-branch element
     */
    public PipelineElement getThenBranch() {
        return thenBranch;
    }

    /**
     * Returns the else-branch.
     *
     * @return the else-branch element, or null if not set
     */
    public PipelineElement getElseBranch() {
        return elseBranch;
    }
}
