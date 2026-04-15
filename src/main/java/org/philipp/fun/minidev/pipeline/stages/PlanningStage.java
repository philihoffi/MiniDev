package org.philipp.fun.minidev.pipeline.stages;

import org.philipp.fun.minidev.pipeline.impl.DefaultStage;
import org.philipp.fun.minidev.pipeline.steps.planning.*;

public class PlanningStage extends DefaultStage {
    public PlanningStage() {
        super("Planning Stage");
        addStep(new ThemeGenerationStep());
        addStep(new IdeaGenerationStep());
        addStep(new NarrowingDownStep());
        addStep(new ConceptElaborationStep());
        addStep(new EvaluationStep());
        addStep(new DetailedDesignStep());
    }
}
