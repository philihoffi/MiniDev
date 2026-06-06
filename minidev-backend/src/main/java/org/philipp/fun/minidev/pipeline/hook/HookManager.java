package org.philipp.fun.minidev.pipeline.hook;

import org.philipp.fun.minidev.pipeline.core.PipelineContext;
import org.philipp.fun.minidev.pipeline.core.PipelineElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class HookManager {
    private static final Logger log = LoggerFactory.getLogger(HookManager.class);
    private final Map<String, List<HookInvocation>> beforeHooks = new ConcurrentHashMap<>();
    private final Map<String, List<HookInvocation>> afterHooks = new ConcurrentHashMap<>();
    private final Map<String, List<AroundInvocation>> aroundHooks = new ConcurrentHashMap<>();

    public HookManager(ApplicationContext ctx) {
        Map<String, Object> hookBeans = ctx.getBeansWithAnnotation(PipelineHook.class);
        for (var entry : hookBeans.entrySet()) {
            Object bean = entry.getValue();
            Class<?> type = bean.getClass();
            PipelineHook hookAnn = type.getAnnotation(PipelineHook.class);
            String[] targets = hookAnn.value();
            if (targets.length == 0) targets = new String[]{"*"};
            registerHooks(bean, type, targets);
        }
    }

    private void registerHooks(Object bean, Class<?> type, String[] targets) {
        for (Method method : type.getMethods()) {
            if (method.isAnnotationPresent(Before.class)) {
                String stage = method.getAnnotation(Before.class).value();
                HookInvocation inv = (e, c) -> invokeMethod(bean, method, e, c);
                for (String t : targets) beforeHooks.computeIfAbsent(t, k -> Collections.synchronizedList(new ArrayList<>())).add(inv);
            }
            if (method.isAnnotationPresent(After.class)) {
                String stage = method.getAnnotation(After.class).value();
                HookInvocation inv = (e, c) -> invokeMethod(bean, method, e, c);
                for (String t : targets) afterHooks.computeIfAbsent(t, k -> Collections.synchronizedList(new ArrayList<>())).add(inv);
            }
            if (method.isAnnotationPresent(Around.class)) {
                String stage = method.getAnnotation(Around.class).value();
                AroundInvocation inv = (e, c, s) -> {
                    try {
                        return (boolean) method.invoke(bean, e, c, s);
                    } catch (Exception ex) {
                        throw new RuntimeException("Around hook failed", ex);
                    }
                };
                for (String t : targets) aroundHooks.computeIfAbsent(t, k -> Collections.synchronizedList(new ArrayList<>())).add(inv);
            }
        }
    }

    private void invokeMethod(Object bean, Method method, PipelineElement element, PipelineContext context) {
        try {
            Object[] args = new Object[method.getParameterCount()];
            for (int i = 0; i < args.length; i++) {
                Class<?> ptype = method.getParameterTypes()[i];
                if (PipelineElement.class.isAssignableFrom(ptype)) {
                    args[i] = element;
                } else if (PipelineContext.class.isAssignableFrom(ptype)) {
                    args[i] = context;
                } else if (boolean.class.equals(ptype) || Boolean.class.equals(ptype)) {
                    args[i] = false;
                } else if (String.class.equals(ptype)) {
                    args[i] = getNameFromElement(element);
                }
            }
            method.invoke(bean, args);
        } catch (Exception e) {
            log.warn("Hook method invocation failed: {}", e.getMessage());
        }
    }

    private static String getNameFromElement(PipelineElement element) {
        return element != null ? element.getName() : null;
    }

    public PipelineElement wrapWithHooks(PipelineElement element, String stageType) {
        String key = stageType != null ? stageType : element.getName();
        List<HookInvocation> befores = new ArrayList<>();
        List<HookInvocation> afters = new ArrayList<>();
        List<AroundInvocation> arounds = new ArrayList<>();

        List<HookInvocation> wb = beforeHooks.get("*");
        if (wb != null) befores.addAll(wb);
        List<HookInvocation> sb = beforeHooks.get(key);
        if (sb != null) befores.addAll(sb);

        List<HookInvocation> wa = afterHooks.get("*");
        if (wa != null) afters.addAll(wa);
        List<HookInvocation> sa = afterHooks.get(key);
        if (sa != null) afters.addAll(sa);

        List<AroundInvocation> wra = aroundHooks.get("*");
        if (wra != null) arounds.addAll(wra);
        List<AroundInvocation> sra = aroundHooks.get(key);
        if (sra != null) arounds.addAll(sra);

        if (befores.isEmpty() && afters.isEmpty() && arounds.isEmpty()) return element;
        return new HookedElement(element, befores, afters, arounds);
    }

    @FunctionalInterface
    private interface HookInvocation {
        void invoke(PipelineElement element, PipelineContext context);
    }

    @FunctionalInterface
    private interface AroundInvocation {
        boolean invoke(PipelineElement element, PipelineContext context, Supplier<Boolean> execution) throws Exception;
    }

    private record HookedElement(PipelineElement delegate,
                                  List<HookInvocation> befores,
                                  List<HookInvocation> afters,
                                  List<AroundInvocation> arounds) implements PipelineElement {
        @Override public String getName() { return delegate.getName(); }
        @Override public List<org.philipp.fun.minidev.pipeline.core.PipelineListener> getListeners() { return delegate.getListeners(); }
        @Override public void setListeners(List<org.philipp.fun.minidev.pipeline.core.PipelineListener> listeners) { delegate.setListeners(listeners); }

        @Override
        public boolean execute(PipelineContext context) throws Exception {
            befores.forEach(h -> h.invoke(delegate, context));
            try {
                boolean result;
                if (!arounds.isEmpty()) {
                    Supplier<Boolean> execution = () -> {
                        try { return delegate.execute(context); }
                        catch (RuntimeException e) { throw e; }
                        catch (Exception e) { throw new RuntimeException(e); }
                    };
                    result = arounds.get(0).invoke(delegate, context, execution);
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