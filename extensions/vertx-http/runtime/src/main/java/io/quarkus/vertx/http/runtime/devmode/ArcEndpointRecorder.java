package io.quarkus.vertx.http.runtime.devmode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableObserverMethod;
import io.quarkus.arc.impl.ArcContainerImpl;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.Json.JsonArrayBuilder;
import io.quarkus.vertx.http.runtime.devmode.Json.JsonObjectBuilder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class ArcEndpointRecorder {

    public Handler<RoutingContext> createBeansHandler() {
        return new Handler<RoutingContext>() {

            @Override
            public void handle(RoutingContext ctx) {
                ctx.response().putHeader("Content-Type", "application/json");

                ArcContainerImpl container = ArcContainerImpl.instance();
                List<InjectableBean<?>> beans = container.getBeans();
                beans.addAll(container.getInterceptors());

                String kindParam = ctx.request().getParam("kind");
                InjectableBean.Kind kind = kindParam != null ? InjectableBean.Kind.valueOf(kindParam) : null;
                String scopeEndsWith = ctx.request().getParam("scope");
                String beanClassStartsWith = ctx.request().getParam("beanClass");

                for (Iterator<InjectableBean<?>> it = beans.iterator(); it.hasNext();) {
                    InjectableBean<?> injectableBean = it.next();
                    if (kind != null && !kind.equals(injectableBean.getKind())) {
                        it.remove();
                    }
                    if (scopeEndsWith != null && !injectableBean.getScope().getName().endsWith(scopeEndsWith)) {
                        it.remove();
                    }
                    if (beanClassStartsWith != null
                            && !injectableBean.getBeanClass().getName().startsWith(beanClassStartsWith)) {
                        it.remove();
                    }
                }

                JsonArrayBuilder array = Json.array();
                for (InjectableBean<?> injectableBean : beans) {
                    JsonObjectBuilder bean = Json.object();
                    bean.put("id", injectableBean.getIdentifier());
                    bean.put("kind", injectableBean.getKind().toString());
                    bean.put("generatedClass", injectableBean.getClass().getName());
                    bean.put("beanClass", injectableBean.getBeanClass().getName());
                    JsonArrayBuilder types = Json.array();
                    for (Type beanType : injectableBean.getTypes()) {
                        types.add(beanType.getTypeName());
                    }
                    bean.put("types", types);
                    JsonArrayBuilder qualifiers = Json.array();
                    for (Annotation qualifier : injectableBean.getQualifiers()) {
                        if (qualifier.annotationType().equals(Any.class) || qualifier.annotationType().equals(Default.class)) {
                            qualifiers.add("@" + qualifier.annotationType().getSimpleName());
                        } else {
                            qualifiers.add(qualifier.toString());
                        }
                    }
                    bean.put("qualifiers", qualifiers);
                    bean.put("scope", injectableBean.getScope().getName());

                    if (injectableBean.getDeclaringBean() != null) {
                        bean.put("declaringBean", injectableBean.getDeclaringBean().getIdentifier());
                    }
                    if (injectableBean.getName() != null) {
                        bean.put("name", injectableBean.getName());
                    }
                    if (injectableBean.isAlternative()) {
                        bean.put("alternativePriority", injectableBean.getAlternativePriority());
                    }
                    if (injectableBean.isDefaultBean()) {
                        bean.put("isDefault", true);
                    }
                    array.add(bean);
                }
                ctx.response().end(array.build());
            }
        };
    }

    public Handler<RoutingContext> createObserversHandler() {
        return new Handler<RoutingContext>() {

            @Override
            public void handle(RoutingContext ctx) {
                ctx.response().putHeader("Content-Type", "application/json");

                ArcContainerImpl container = ArcContainerImpl.instance();
                List<InjectableObserverMethod<?>> observers = container.getObservers();

                JsonArrayBuilder array = Json.array();
                for (InjectableObserverMethod<?> injectableObserver : observers) {
                    JsonObjectBuilder observer = Json.object();
                    observer.put("generatedClass", injectableObserver.getClass().getName());
                    observer.put("observedType", injectableObserver.getObservedType().getTypeName());
                    if (!injectableObserver.getObservedQualifiers().isEmpty()) {
                        JsonArrayBuilder qualifiers = Json.array();
                        for (Annotation qualifier : injectableObserver.getObservedQualifiers()) {
                            qualifiers.add(qualifier.toString());
                        }
                        observer.put("qualifiers", qualifiers);
                    }
                    observer.put("priority", injectableObserver.getPriority());
                    observer.put("reception", injectableObserver.getReception().toString());
                    observer.put("transactionPhase", injectableObserver.getTransactionPhase().toString());
                    observer.put("async", injectableObserver.isAsync());
                    if (injectableObserver.getDeclaringBeanIdentifier() != null) {
                        observer.put("declaringBean", injectableObserver.getDeclaringBeanIdentifier());
                    }
                    array.add(observer);
                }
                ctx.response().end(array.build());
            }
        };
    }

}
