package org.philipp.fun.minidev.pipeline.hook;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level annotation that marks a hook method to be invoked after the
 * specified pipeline node.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface After {

    /**
     * The name of the pipeline node to hook after.
     *
     * @return the node name
     */
    String value();
}