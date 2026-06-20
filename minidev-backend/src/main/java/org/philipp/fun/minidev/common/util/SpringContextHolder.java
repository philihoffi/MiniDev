package org.philipp.fun.minidev.common.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Holder that exposes the Spring {@link ApplicationContext} for access in
 * non-managed beans.
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    /** The application context, set once on startup. */
    private static volatile ApplicationContext context;

    /**
     * Sets the application context (called by Spring).
     *
     * @param applicationContext the application context
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    /**
     * Retrieves a bean of the given type from the application context.
     *
     * @param <T>     the bean type
     * @param beanType the class of the bean
     * @return the bean instance, or {@code null} if the context is not available
     */
    public static <T> T getBean(Class<T> beanType) {
        ApplicationContext current = context;
        if (current == null) {
            return null;
        }
        return current.getBean(beanType);
    }
}