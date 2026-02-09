package example.config;

import com.posadskiy.restsecurity.audit.SecurityAuditListener;
import com.posadskiy.restsecurity.context.SecurityContext;
import com.posadskiy.restsecurity.exception.RestSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditListenerImpl implements SecurityAuditListener {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditListenerImpl.class);

    @Override
    public void onAuthenticationSuccess(SecurityContext context, String method) {
        log.info("Auth success: userId={}, method={}", context.userId(), method);
    }

    @Override
    public void onAuthenticationFailure(String sessionId, String method, RestSecurityException exception) {
        log.warn("Auth failure: sessionId={}, method={}, error={}", sessionId, method, exception.getMessage());
    }
}
