package com.posadskiy.restsecurity.spring;

import com.posadskiy.restsecurity.annotation.Public;
import com.posadskiy.restsecurity.annotation.Security;
import com.posadskiy.restsecurity.audit.SecurityAuditListener;
import com.posadskiy.restsecurity.context.SecurityContext;
import com.posadskiy.restsecurity.context.SecurityContextHolder;
import com.posadskiy.restsecurity.enforcer.SecurityEnforcer;
import com.posadskiy.restsecurity.exception.RestSecurityException;
import com.posadskiy.restsecurity.rest.SecuredRequestContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Wraps beans that have @Security-annotated methods in a proxy that enforces
 * session validity, user existence, roles, and same-user access before invoking the method.
 * Populates {@link SecurityContextHolder} for the duration of the call.
 * Delegates validation to {@link SecurityEnforcer}.
 */
public class SecurityAnnotationBeanPostProcessor implements BeanPostProcessor {

    private final SecurityEnforcer enforcer;
    private final Map<String, Class<?>> beansToProxy = new HashMap<>();
    private SecurityAuditListener auditListener;

    public SecurityAnnotationBeanPostProcessor(com.posadskiy.restsecurity.controller.SessionSecurityController sessionController,
                                               com.posadskiy.restsecurity.controller.UserSecurityController userController) {
        this.enforcer = new SecurityEnforcer(sessionController, userController);
    }

    @Autowired(required = false)
    public void setAuditListener(SecurityAuditListener auditListener) {
        this.auditListener = auditListener;
        this.enforcer.setAuditListener(auditListener);
    }

    @Override
    public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();

        if (beanClass.getDeclaredAnnotation(Security.class) != null) {
            beansToProxy.put(beanName, beanClass);
            return bean;
        }

        for (Method method : beanClass.getDeclaredMethods()) {
            if (method.getDeclaredAnnotation(Security.class) != null) {
                beansToProxy.put(beanName, beanClass);
                break;
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> beanClass = beansToProxy.get(beanName);
        if (beanClass == null) {
            return bean;
        }

        Class<?>[] interfaces = bean.getClass().getInterfaces();
        if (interfaces.length == 0) {
            return bean;
        }

        return Proxy.newProxyInstance(bean.getClass().getClassLoader(), interfaces, (proxy, method, args) -> {
            Method declaredMethod = findMethodInHierarchy(beanClass, method.getName(), method.getParameterTypes());
            if (declaredMethod == null) {
                return invoke(bean, method, args);
            }

            if (declaredMethod.isAnnotationPresent(Public.class)) {
                return invoke(bean, method, args);
            }

            Security security = declaredMethod.getDeclaredAnnotation(Security.class);
            if (security == null) {
                security = beanClass.getDeclaredAnnotation(Security.class);
            }

            if (security != null) {
                if (args == null || args.length == 0 || !(args[0] instanceof SecuredRequestContext ctx)) {
                    throw new IllegalArgumentException(
                            "@Security method '" + method.getName() + "' first parameter must be SecuredRequestContext");
                }

                String methodName = beanClass.getSimpleName() + "." + method.getName();
                return executeSecured(bean, method, args, ctx, security.roles(), methodName);
            }

            return invoke(bean, method, args);
        });
    }

    private Object executeSecured(Object bean, Method method, Object[] args,
                                  SecuredRequestContext ctx, String[] requiredRoles,
                                  String methodName) throws Throwable {
        String sessionId = ctx.getSessionId();
        try {
            SecurityContext securityContext = enforcer.validateAndBuildContext(ctx, requiredRoles);
            SecurityContextHolder.setContext(securityContext);

            if (auditListener != null) {
                auditListener.onAuthenticationSuccess(securityContext, methodName);
            }

            return invoke(bean, method, args);
        } catch (RuntimeException e) {
            if (e instanceof RestSecurityException secEx && auditListener != null) {
                auditListener.onAuthenticationFailure(sessionId, methodName, secEx);
            }
            throw e;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Finds a method in the class hierarchy (bean class and superclasses).
     * Uses getMethod first for public methods, then walks getDeclaredMethod for each class.
     */
    private static Method findMethodInHierarchy(Class<?> beanClass, String methodName, Class<?>[] parameterTypes) {
        try {
            return beanClass.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            for (Class<?> c = beanClass; c != null; c = c.getSuperclass()) {
                try {
                    return c.getDeclaredMethod(methodName, parameterTypes);
                } catch (NoSuchMethodException ignored) {
                    // continue to superclass
                }
            }
            return null;
        }
    }

    private static Object invoke(Object bean, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(bean, args);
        } catch (InvocationTargetException e) {
            throw e.getCause() != null ? e.getCause() : e;
        }
    }
}
