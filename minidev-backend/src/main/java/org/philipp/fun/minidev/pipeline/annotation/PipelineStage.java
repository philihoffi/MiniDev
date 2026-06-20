package org.philipp.fun.minidev.pipeline.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * Annotation that marks a class as a pipeline stage, automatically registering
 * it as a Spring component.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface PipelineStage {

    /**
     * The unique name of this pipeline stage.
     *
     * @return the stage name
     */
    String value();
}