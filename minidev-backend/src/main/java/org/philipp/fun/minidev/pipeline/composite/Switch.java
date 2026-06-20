package org.philipp.fun.minidev.pipeline.composite;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;

/**
 * A composite pipeline element that selects a branch based on conditions.
 */
public class Switch extends BaseElement {

    /** List of cases to evaluate. */
    private final List<Case> cases = new ArrayList<>();

    /** Default branch if no case matches. */
    private PipelineElement defaultBranch;

    /**
     * Constructs a Switch with a given name.
     *
     * @param name the element name
     */
    public Switch(String name) {
        super(name);
    }

    /**
     * Adds a case to the switch.
     *
     * @param condition the condition predicate
     * @param branch    the branch element
     * @return this switch for chaining
     */
    public Switch addCase(Predicate<PipelineContext> condition, PipelineElement branch) {
        cases.add(new Case(condition, branch));
        return this;
    }

    /**
     * Sets the default branch.
     *
     * @param branch the default branch element
     * @return this switch for chaining
     */
    public Switch defaultBranch(PipelineElement branch) {
        this.defaultBranch = branch;
        return this;
    }

    /**
     * Executes the switch by evaluating conditions in order.
     *
     * @param ctx the pipeline context
     * @return true if the selected branch succeeded, false otherwise
     * @throws Exception if an error occurs
     */
    @Override
    public boolean execute(PipelineContext ctx) throws Exception {
        for (Case c : cases) {
            if (c.condition.test(ctx)) {
                return runElement(c.branch, ctx);
            }
        }
        if (defaultBranch != null) {
            return runElement(defaultBranch, ctx);
        }
        return true;
    }

    /**
     * Returns an unmodifiable list of cases.
     *
     * @return the list of cases
     */
    public List<Case> getCases() {
        return List.copyOf(cases);
    }

    /**
     * Returns the default branch.
     *
     * @return the default branch element
     */
    public PipelineElement getDefaultBranch() {
        return defaultBranch;
    }

    /**
     * A case in a switch statement.
     *
     * @param condition the condition predicate
     * @param branch    the branch element
     */
    public record Case(Predicate<PipelineContext> condition, PipelineElement branch) {

        /**
         * Compact constructor that validates inputs.
         */
        public Case {
            Objects.requireNonNull(condition);
            Objects.requireNonNull(branch);
        }
    }
}