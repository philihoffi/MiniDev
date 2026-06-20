package org.philipp.fun.minidev.pipeline.hook;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.philipp.fun.minidev.pipeline.core.PipelineListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Manages before/after/around hooks for pipeline elements.
 */
@Component
public class HookManager {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(HookManager.class);

    /** Map of before hooks keyed by target stage name. */
    private final Map<String, List<HookInvocation>> beforeHooks =
            new ConcurrentHashMap<>();

    /** Map of after hooks keyed by target stage name. */
    private final Map<String, List<HookInvocation>> afterHooks =
            new ConcurrentHashMap<>();

    /** Map of around hooks keyed by target stage name. */
    private final Map<String, List<AroundInvocation>> aroundHooks =
            new ConcurrentHashMap<>();

    /**
     * Constructs a HookManager and scans the application context for
     * {@link PipelineHook}-annotated beans.
     *
     * @param ctx the application context
     */
    public HookManager(ApplicationContext ctx) {
        Map<String, Object> hookBeans = ctx.getBeansWithAnnotation(PipelineHook.class);
        for (var entry : hookBeans.entrySet()) {
            Object bean = entry.getValue();
            Class<?> type = bean.getClass();
            PipelineHook hookAnn = type.getAnnotation(PipelineHook.class);
            String[] targets = hookAnn.value();
            if (targets.length == 0) {
                targets = new String[]{"*"};
            }
            registerHooks(bean, type, targets);
        }
    }

    /**
     * Registers all hook methods from a bean for the given targets.
     *
     * @param bean    the hook bean instance
     * @param type    the bean class
     * @param targets the target stage names
     */
    private void registerHooks(Object bean, Class<?> type, String[] targets) {
        for (Method method : type.getMethods()) {
            if (method.isAnnotationPresent(Before.class)) {
                HookInvocation inv = (e, c) -> invokeMethod(bean, method, e, c);
                for (String t : targets) {
                    beforeHooks
                            .computeIfAbsent(
                                    t,
                                    k -> Collections.synchronizedList(
                                            new ArrayList<>()))
                            .add(inv);
                }
            }
            if (method.isAnnotationPresent(After.class)) {
                HookInvocation inv = (e, c) -> invokeMethod(bean, method, e, c);
                for (String t : targets) {
                    afterHooks
                            .computeIfAbsent(
                                    t,
                                    k -> Collections.synchronizedList(
                                            new ArrayList<>()))
                            .add(inv);
                }
            }
            if (method.isAnnotationPresent(Around.class)) {
                AroundInvocation inv = (e, c, s) -> {
                    try {
                        return (boolean) method.invoke(bean, e, c, s);
                    } catch (Exception ex) {
                        throw new RuntimeException("Around hook failed", ex);
                    }
                };
                for (String t : targets) {
                    aroundHooks
                            .computeIfAbsent(
                                    t,
                                    k -> Collections.synchronizedList(
                                            new ArrayList<>()))
                            .add(inv);
                }
            }
        }
    }

    /**
     * Invokes a hook method reflectively, matching parameters by type.
     *
     * @param bean    the hook bean
     * @param method  the method to invoke
     * @param element the pipeline element
     * @param context the pipeline context
     */
    private void invokeMethod(
            Object bean, Method method,
            PipelineElement element, PipelineContext context) {
        try {
            Object[] args = new Object[method.getParameterCount()];
            for (int i = 0; i < args.length; i++) {
                Class<?> ptype = method.getParameterTypes()[i];
                if (PipelineElement.class.isAssignableFrom(ptype)) {
                    args[i] = element;
                } else if (PipelineContext.class.isAssignableFrom(ptype)) {
                    args[i] = context;
                } else if (boolean.class.equals(ptype)
                        || Boolean.class.equals(ptype)) {
                    args[i] = false;
                } else if (String.class.equals(ptype)) {
                    args[i] = getNameFromElement(element);
                }
            }
            method.invoke(bean, args);
        } catch (Exception e) {
            LOG.warn("Hook method invocation failed: {}", e.getMessage());
        }
    }

    /**
     * Extracts the name from a pipeline element.
     *
     * @param element the element
     * @return the name, or null
     */
    private static String getNameFromElement(PipelineElement element) {
        return element != null ? element.getName() : null;
    }

    /**
     * Wraps a pipeline element with applicable hooks.
     *
     * @param element   the element to wrap
     * @param stageType the stage type key for hook matching
     * @return the wrapped element, or the original if no hooks apply
     */
    public PipelineElement wrapWithHooks(
            PipelineElement element, String stageType) {
        String key = stageType != null ? stageType : element.getName();
        List<HookInvocation> befores = new ArrayList<>();
        List<HookInvocation> afters = new ArrayList<>();
        List<AroundInvocation> arounds = new ArrayList<>();

        List<HookInvocation> wb = beforeHooks.get("*");
        if (wb != null) {
            befores.addAll(wb);
        }
        List<HookInvocation> sb = beforeHooks.get(key);
        if (sb != null) {
            befores.addAll(sb);
        }

        List<HookInvocation> wa = afterHooks.get("*");
        if (wa != null) {
            afters.addAll(wa);
        }
        List<HookInvocation> sa = afterHooks.get(key);
        if (sa != null) {
            afters.addAll(sa);
        }

        List<AroundInvocation> wra = aroundHooks.get("*");
        if (wra != null) {
            arounds.addAll(wra);
        }
        List<AroundInvocation> sra = aroundHooks.get(key);
        if (sra != null) {
            arounds.addAll(sra);
        }

        if (befores.isEmpty() && afters.isEmpty() && arounds.isEmpty()) {
            return element;
        }
        return new HookedElement(element, befores, afters, arounds);
    }

    /** Functional interface for before/after hook invocations. */
    @FunctionalInterface
    private interface HookInvocation {
        /**
         * Invokes the hook.
         *
         * @param element the pipeline element
         * @param context the pipeline context
         */
        void invoke(PipelineElement element, PipelineContext context);
    }

    /** Functional interface for around hook invocations. */
    @FunctionalInterface
    private interface AroundInvocation {
        /**
         * Invokes the around hook.
         *
         * @param element   the pipeline element
         * @param context   the pipeline context
         * @param execution the original execution supplier
         * @return the result of the execution
         */
        boolean invoke(
                PipelineElement element,
                PipelineContext context,
                Supplier<Boolean> execution) throws Exception;
    }

    /**
     * A pipeline element wrapper that applies before/after/around hooks.
     *
     * @param delegate the delegate element
     * @param befores  the before hooks
     * @param afters   the after hooks
     * @param arounds  the around hooks
     */
    private record HookedElement(
            PipelineElement delegate,
            List<HookInvocation> befores,
            List<HookInvocation> afters,
            List<AroundInvocation> arounds) implements PipelineElement {

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public List<PipelineListener> getListeners() {
            return delegate.getListeners();
        }

        @Override
        public void setListeners(List<PipelineListener> listeners) {
            delegate.setListeners(listeners);
        }

        @Override
        public boolean execute(PipelineContext context) throws Exception {
            befores.forEach(h -> h.invoke(delegate, context));
            try {
                boolean result;
                if (!arounds.isEmpty()) {
                    Supplier<Boolean> execution = () -> {
                        try {
                            return delegate.execute(context);
                        } catch (RuntimeException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    };
                    result = arounds.get(0).invoke(
                            delegate, context, execution);
                } else {
                    result = delegate.execute(context);
                }
                afters.forEach(h -> h.invoke(delegate, context));
                return result;
            } catch (RuntimeException e) {
                throw e;
            }
        }
    }
}