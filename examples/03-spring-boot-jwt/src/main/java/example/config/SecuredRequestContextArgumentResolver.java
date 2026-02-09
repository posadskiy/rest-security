package example.config;

import com.posadskiy.restsecurity.rest.SecuredRequest;
import com.posadskiy.restsecurity.rest.SecuredRequestContext;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves controller method parameter of type SecuredRequestContext from the current HTTP request.
 * Reads "Authorization: Bearer &lt;token&gt;" and builds SecuredRequest(token).
 */
public class SecuredRequestContextArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return SecuredRequestContext.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return new SecuredRequest(null);
        }
        String auth = request.getHeader("Authorization");
        String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7).trim() : null;
        return new SecuredRequest(token);
    }
}
