package org.philipp.fun.minidev.pipeline.stages;

import org.philipp.fun.minidev.pipeline.impl.SequenzStage;
import org.philipp.fun.minidev.pipeline.steps.planning.*;

public class PlanningStage extends SequenzStage {
    public PlanningStage() {
        super("Planning Stage");
        addElement(new ThemeGenerationStep());
        addElement(new IdeaGenerationStep());
        addElement(new NarrowingDownStep());
        addElement(new ConceptElaborationStep());
        addElement(new EvaluationStep());
        addElement(new DetailedDesignStep());
    }
}
