package example.config;

import com.posadskiy.restsecurity.rest.SecuredRequest;
import com.posadskiy.restsecurity.rest.SecuredRequestContext;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Resolves SecuredRequestContext from cookie "SESSION_ID" and optional path variable "userId" for same-user checks.
 */
public class SessionIdArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String SESSION_COOKIE = "SESSION_ID";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return SecuredRequestContext.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String sessionId = null;
        if (request != null && request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (SESSION_COOKIE.equals(c.getName())) {
                    sessionId = c.getValue();
                    break;
                }
            }
        }
        @SuppressWarnings("unchecked")
        Map<String, String> pathVars = (Map<String, String>) webRequest.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        String targetUserId = pathVars != null ? pathVars.get("userId") : null;
        return new SecuredRequest(sessionId, targetUserId, null);
    }
}
