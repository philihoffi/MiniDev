package org.philipp.fun.minidev.pipeline.hook;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level annotation that marks a hook method to be invoked both before
 * and after the specified pipeline node.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Around {

    /**
     * The name of the pipeline node to wrap.
     *
     * @return the node name
     */
    String value();
}