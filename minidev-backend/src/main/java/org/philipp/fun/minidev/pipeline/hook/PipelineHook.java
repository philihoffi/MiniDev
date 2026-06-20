package org.philipp.fun.minidev.pipeline.hook;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * Annotation that marks a class as a pipeline hook, automatically registering
 * it as a Spring component.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface PipelineHook {

    /**
     * Optional list of pipeline names this hook applies to.
     *
     * @return the pipeline names
     */
    String[] value() default {};
}