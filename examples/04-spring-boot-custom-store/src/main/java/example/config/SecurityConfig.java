package example.config;

import com.posadskiy.restsecurity.spring.SecurityAnnotationBeanPostProcessor;
import example.security.DatabaseUserController;
import example.security.RedisSessionController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    @Bean
    public RedisSessionController redisSessionController() {
        return new RedisSessionController();
    }

    @Bean
    public DatabaseUserController databaseUserController() {
        return new DatabaseUserController();
    }

    @Bean
    public SecurityAnnotationBeanPostProcessor securityAnnotationBeanPostProcessor(
            RedisSessionController sessionController,
            DatabaseUserController userController) {
        return new SecurityAnnotationBeanPostProcessor(sessionController, userController);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new SessionIdArgumentResolver());
    }
}
