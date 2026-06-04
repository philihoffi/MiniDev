package org.philipp.fun.minidev.pipeline;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class BaseElement implements PipelineElement {
    private final String name;
    private List<PipelineListener> listeners = Collections.emptyList();

    protected BaseElement(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override public String getName() { return name; }
    @Override public List<PipelineListener> getListeners() { return Collections.unmodifiableList(listeners); }
    @Override public void setListeners(List<PipelineListener> listeners) { 
        this.listeners = listeners != null ? listeners : Collections.emptyList(); 
    }

    protected void notifyStart(PipelineElement e, PipelineContext ctx) { listeners.forEach(l -> l.onStart(e, ctx)); }
    protected void notifyEnd(PipelineElement e, PipelineContext ctx, boolean s) { listeners.forEach(l -> l.onEnd(e, ctx, s)); }
    protected void notifyWarning(PipelineElement e, PipelineContext ctx, String m) { listeners.forEach(l -> l.onWarning(e, ctx, m)); }
    protected void notifyError(PipelineElement e, PipelineContext ctx, Exception ex) { listeners.forEach(l -> l.onError(e, ctx, ex)); }

    protected boolean runElement(PipelineElement element, PipelineContext context) {
        element.setListeners(this.listeners);
        notifyStart(element, context);
        try {
            boolean result = element.execute(context);
            notifyEnd(element, context, result);
            return result;
        } catch (Exception e) {
            notifyError(element, context, e);
            return false;
        }
    }
}
