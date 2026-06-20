package org.philipp.fun.minidev.pipeline.hook;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level annotation that marks a hook method to be invoked before the
 * specified pipeline node.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Before {

    /**
     * The name of the pipeline node to hook before.
     *
     * @return the node name
     */
    String value();
}