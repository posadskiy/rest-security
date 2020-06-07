package com.posadskiy.restsecurity.annotation.bpp;

import com.posadskiy.restsecurity.controller.SessionSecurityController;
import com.posadskiy.restsecurity.annotation.bpp.mock.TestClassWithSecurityAnnotation;
import com.posadskiy.restsecurity.annotation.bpp.mock.TestClassWithSecurityAnnotationImpl;
import com.posadskiy.restsecurity.annotation.bpp.mock.TestClassWithoutSecurityAnnotation;
import com.posadskiy.restsecurity.controller.UserSecurityController;
import com.posadskiy.restsecurity.exception.*;
import com.posadskiy.restsecurity.rest.RequestWrapper;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecurityAnnotationBeanPostProcessorTest {
	private static final String SESSION_ID = "SESSION_ID";
	private static final String USER_ID = "USER_ID";
	private static final String ANOTHER_USER_ID = "ANOTHER_USER_ID";
	private static final int SESSION_TIME = 0;
	private static final String USER_ROLE = "USER";
	private static final String ADMIN_ROLE = "ADMIN";
	private static final String BAD_ROLE = "BAD_ROLE";
	private static final List<String> EMPTY_ROLES = Arrays.asList();
	private static final List<String> BAD_ROLES = Arrays.asList(BAD_ROLE);
	private static final List<String> USER_ROLES = Arrays.asList(USER_ROLE);
	private static final List<String> ADMIN_ROLES = Arrays.asList(ADMIN_ROLE);

	private final SecurityAnnotationBeanPostProcessor postProcessor = new SecurityAnnotationBeanPostProcessor();
	private final SessionSecurityController sessionController = mock(SessionSecurityController.class);
	private final UserSecurityController userController = mock(UserSecurityController.class);

	@Before
	public void beforeTests() throws IllegalAccessException {
		FieldUtils.writeField(postProcessor, "sessionController", sessionController, true);
		FieldUtils.writeField(postProcessor, "userController", userController, true);
	}

	@Test
	public void postProcess_BeanWithAnnotation_ProxyReturned() {
		TestClassWithSecurityAnnotationImpl bean = new TestClassWithSecurityAnnotationImpl();
		String beanName = "";

		Object beanBeforeInitialization = postProcessor.postProcessBeforeInitialization(bean, beanName);
		Object beanAfterInitialization = postProcessor.postProcessAfterInitialization(beanBeforeInitialization, beanName);

		assertNotEquals(bean.getClass().getSimpleName(), beanAfterInitialization.getClass().getSimpleName());
	}

	@Test
	public void postProcess_BeanWithoutAnnotation_OriginalBeanReturned() {
		TestClassWithoutSecurityAnnotation bean = new TestClassWithoutSecurityAnnotation();
		String beanName = "";

		Object beanBeforeInitialization = postProcessor.postProcessBeforeInitialization(bean, beanName);
		Object beanAfterInitialization = postProcessor.postProcessAfterInitialization(beanBeforeInitialization, beanName);

		assertEquals(bean.getClass().getSimpleName(), beanAfterInitialization.getClass().getSimpleName());
	}

	@Test(expected = SessionDoesNotExistException.class)
	public void postProcess_SessionDoesNotExistInSessionDb_SessionDoesNotExistExceptionThrew() {
		when(sessionController.isSessionExist(SESSION_ID)).thenReturn(false);
		TestClassWithSecurityAnnotationImpl bean = new TestClassWithSecurityAnnotationImpl();
		String beanName = "";
		RequestWrapper requestWrapper = new RequestWrapper().sessionId(SESSION_ID);

		TestClassWithSecurityAnnotation beanBeforeInitialization =
			(TestClassWithSecurityAnnotation) postProcessor.postProcessBeforeInitialization(bean, beanName);
		TestClassWithSecurityAnnotation beanAfterInitialization =
			(TestClassWithSecurityAnnotation) postProcessor.postProcessAfterInitialization(beanBeforeInitialization, beanName);

		beanAfterInitialization.testMethod(requestWrapper);
	}

	@Test(expected = SessionExpiredException.class)
	public void postProcess_SessionIsExpiredInSessionDb_SessionExpiredExceptionThrew() {
		when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
		when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(true);
		TestClassWithSecurityAnnotationImpl bean = new TestClassWithSecurityAnnotationImpl();
		String beanName = "";
		RequestWrapper requestWrapper = new RequestWrapper().sessionId(SESSION_ID);

		TestClassWithSecurityAnnotation beanBeforeInitialization =
			(TestClassWithSecurityAnnotation) postProcessor.postProcessBeforeInitialization(bean, beanName);
		TestClassWithSecurityAnnotation beanAfterInitialization =
			(TestClassWithSecurityAnnotation) postProcessor.postProcessAfterInitialization(beanBeforeInitialization, beanName);

		beanAfterInitialization.testMethod(requestWrapper);
	}

	@Test(expected = UserDoesNotExistException.class)
	public void postProcess_UserDoesNotExistInDb_UserDoesNotExistExceptionThrew() {
		when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
		when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
		when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
		when(userController.isUserExist(USER_ID)).thenReturn(false);

		TestClassWithSecurityAnnotationImpl bean = new TestClassWithSecurityAnnotationImpl();
		String beanName = "";
		RequestWrapper requestWrapper = new RequestWrapper().sessionId(SESSION_ID);

		TestClassWithSecurityAnnotation beanBeforeInitialization =
			(TestClassWithSecurityAnnotation) postProcessor.postProcessBeforeInitialization(bean, beanName);
		TestClassWithSecurityAnnotation beanAfterInitialization =
			(TestClassWithSecurityAnnotation) postProcessor.postProcessAfterInitialization(beanBeforeInitialization, beanName);

		beanAfterInitialization.testMethod(requestWrapper);
	}

	@Test(expected = UserRolesDoesNotExistException.class)
	public void postProcess_UserRolesDoesNotExist_UserRolesDoesNotExistExceptionThrew() {
		when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
		when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
		when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
		when(userController.isUserExist(USER_ID)).thenReturn(true);
		when(userController.getUserRoles(USER_ID)).thenReturn(EMPTY_ROLES);

		TestClassWithSecurityAnnotationImpl bean = new TestClassWithSecurityAnnotationImpl();
		String beanName = "";
		RequestWrapper requestWrapper = new RequestWrapper().sessionId(SESSION_ID);

		TestClassWithSecurityAnnotation beanBeforeInitialization =
			(TestClassWithSecurityAnnotation) postProcessor.postProcessBeforeInitialization(bean, beanName);
		TestClassWithSecurityAnnotation beanAfterInitialization =
			(TestClassWithSecurityAnnotation) postProcessor.postProcessAfterInitialization(beanBeforeInitialization, beanName);

		beanAfterInitialization.testMethod(requestWrapper);
	}

	@Test(expected = PermissionIsAbsentException.class)
	public void postProcess_DbUserWithoutAccessibleRole_PermissionIsAbsentExceptionThrew() {
		when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
		when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
		when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
		when(userController.isUserExist(USER_ID)).thenReturn(true);
		when(userController.getUserRoles(USER_ID)).thenReturn(BAD_ROLES);

		TestClassWithSecurityAnnotationImpl bean = new TestClassWithSecurityAnnotationImpl();
		String beanName = "";
		RequestWrapper requestWrapper = new RequestWrapper().sessionId(SESSION_ID);

		TestClassWithSecurityAnnotation beanBeforeInitialization =
			(TestClassWithSecurityAnnotation) postProcessor.postProcessBeforeInitialization(bean, beanName);
		TestClassWithSecurityAnnotation beanAfterInitialization =
			(TestClassWithSecurityAnnotation) postProcessor.postProcessAfterInitialization(beanBeforeInitialization, beanName);

		beanAfterInitialization.testMethod(requestWrapper);
	}

	@Test(expected = PermissionForGetAnotherUserIsAbsentException.class)
	public void postProcess_RequestAnotherUserWithoutPermission_PermissionForGetAnotherUserIsAbsentExceptionThrew() {
		when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
		when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
		when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
		when(userController.isUserExist(USER_ID)).thenReturn(true);
		when(userController.getUserRoles(USER_ID)).thenReturn(USER_ROLES);

		TestClassWithSecurityAnnotationImpl bean = new TestClassWithSecurityAnnotationImpl();
		String beanName = "";
		RequestWrapper requestWrapper = new RequestWrapper().sessionId(SESSION_ID).userId(ANOTHER_USER_ID);

		TestClassWithSecurityAnnotation beanBeforeInitialization =
			(TestClassWithSecurityAnnotation) postProcessor.postProcessBeforeInitialization(bean, beanName);
		TestClassWithSecurityAnnotation beanAfterInitialization =
			(TestClassWithSecurityAnnotation) postProcessor.postProcessAfterInitialization(beanBeforeInitialization, beanName);

		beanAfterInitialization.testMethod(requestWrapper);
	}

	@Test
	public void postProcess_PositiveWay_BeanReturned() {
		when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
		when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
		when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
		when(userController.isUserExist(USER_ID)).thenReturn(true);
		when(userController.getUserRoles(USER_ID)).thenReturn(ADMIN_ROLES);

		TestClassWithSecurityAnnotationImpl bean = new TestClassWithSecurityAnnotationImpl();
		String beanName = "";
		RequestWrapper requestWrapper = new RequestWrapper().sessionId(SESSION_ID).userId(ANOTHER_USER_ID);

		TestClassWithSecurityAnnotation beanBeforeInitialization =
			(TestClassWithSecurityAnnotation) postProcessor.postProcessBeforeInitialization(bean, beanName);
		TestClassWithSecurityAnnotation beanAfterInitialization =
			(TestClassWithSecurityAnnotation) postProcessor.postProcessAfterInitialization(beanBeforeInitialization, beanName);

		beanAfterInitialization.testMethod(requestWrapper);
	}
}
