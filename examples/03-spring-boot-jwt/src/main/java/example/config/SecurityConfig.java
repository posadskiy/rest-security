package example.config;

import com.posadskiy.restsecurity.jwt.JwtConfig;
import com.posadskiy.restsecurity.jwt.JwtSecurityController;
import com.posadskiy.restsecurity.spring.SecurityAnnotationBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    private static final String HMAC_SECRET = "spring-boot-jwt-example-secret-at-least-256-bits-for-hs256";

    @Bean
    public JwtSecurityController jwtSecurityController() {
        return new JwtSecurityController(JwtConfig.withSecret(HMAC_SECRET));
    }

    @Bean
    public SecurityAnnotationBeanPostProcessor securityAnnotationBeanPostProcessor(
            JwtSecurityController jwtSecurityController) {
        return new SecurityAnnotationBeanPostProcessor(jwtSecurityController, jwtSecurityController);
    }

    @Bean
    public SecurityAuditListenerImpl securityAuditListener() {
        return new SecurityAuditListenerImpl();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new SecuredRequestContextArgumentResolver());
    }
}
