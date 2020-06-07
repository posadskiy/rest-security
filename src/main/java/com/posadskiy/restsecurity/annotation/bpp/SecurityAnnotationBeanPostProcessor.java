package com.posadskiy.restsecurity.annotation.bpp;

import com.posadskiy.restsecurity.annotation.Security;
import com.posadskiy.restsecurity.controller.SessionSecurityController;
import com.posadskiy.restsecurity.controller.UserSecurityController;
import com.posadskiy.restsecurity.enumiration.Role;
import com.posadskiy.restsecurity.exception.*;
import com.posadskiy.restsecurity.rest.RequestWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public class SecurityAnnotationBeanPostProcessor implements BeanPostProcessor {

	@Autowired
	private SessionSecurityController sessionController;

	@Autowired
	private UserSecurityController userController;

	private final Map<String, Class> beans = new HashMap<>();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		for (Method method : bean.getClass().getMethods()) {
			if (method.getDeclaredAnnotation(Security.class) != null) {
				beans.put(beanName, bean.getClass());
			}
		}

		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

		Class annotatedBean = beans.get(beanName);
		if (annotatedBean != null) {
			return Proxy.newProxyInstance(bean.getClass().getClassLoader(), bean.getClass().getInterfaces(), (proxy, method, args) -> {
				Method declaredMethod = bean.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
				if (declaredMethod.isAnnotationPresent(Security.class)) {
					String[] roles = declaredMethod.getAnnotation(Security.class).roles();
					RequestWrapper requestWrapper = (RequestWrapper) args[0];
					if (!sessionController.isSessionExist(requestWrapper.getSessionId())) {
						throw new SessionDoesNotExistException();
					}
					if (sessionController.isSessionExpired(requestWrapper.getSessionId())) {
						throw new SessionExpiredException();
					}

					final String userId = sessionController.getUserIdBySessionId(requestWrapper.getSessionId());
					boolean isUserExist = userController.isUserExist(userId);
					if (!isUserExist) {
						throw new UserDoesNotExistException();
					}

					List<String> userRoles = userController.getUserRoles(userId);
					if (userRoles == null || userRoles.size() == 0) {
						throw new UserRolesDoesNotExistException();
					}

					if (!userRoles.contains(Role.ADMIN.getRole())) {
						Object[] intersectionRoles = new HashSet<>(userRoles).stream().filter(Arrays.asList(roles)::contains).toArray();
						if (intersectionRoles.length == 0) {
							throw new PermissionIsAbsentException();
						}

						if (requestWrapper.getUserId() != null && !requestWrapper.getUserId().equals(userId)) {
							throw new PermissionForGetAnotherUserIsAbsentException();
						}
					}

					try {
						return method.invoke(bean, args);
					} catch (InvocationTargetException exception) {
						throw exception.getCause();
					}
				} else {
					try {
						return method.invoke(bean, args);
					} catch (InvocationTargetException exception) {
						throw exception.getCause();
					}
				}
			});
		}
		return bean;
	}
}
