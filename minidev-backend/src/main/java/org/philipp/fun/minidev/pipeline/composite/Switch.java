package org.philipp.fun.minidev.pipeline.composite;

import org.philipp.fun.minidev.pipeline.core.BaseElement;
import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class Switch extends BaseElement {
    private final List<Case> cases = new ArrayList<>();
    private PipelineElement defaultBranch;

    public Switch(String name) {
        super(name);
    }

    public Switch addCase(Predicate<PipelineContext> condition, PipelineElement branch) {
        cases.add(new Case(condition, branch));
        return this;
    }

    public Switch defaultBranch(PipelineElement branch) {
        this.defaultBranch = branch;
        return this;
    }

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

    public List<Case> getCases() { return List.copyOf(cases); }
    public PipelineElement getDefaultBranch() { return defaultBranch; }

    public record Case(Predicate<PipelineContext> condition, PipelineElement branch) {
        public Case {
            Objects.requireNonNull(condition);
            Objects.requireNonNull(branch);
        }
    }
}