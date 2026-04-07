package com.soen345.project.testsupport;

import com.soen345.project.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class MvcSecuritySupport {

    private MvcSecuritySupport() {
    }

    /**
     * With {@code addFilters = false}, the security filter chain does not populate
     * {@link SecurityContextHolder}; {@code @AuthenticationPrincipal} still reads from it,
     * so we set both the holder and the standard session attribute for consistency.
     */
    public static RequestPostProcessor principalAsUser(User user) {
        UsernamePasswordAuthenticationToken auth =
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            request.getSession().setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context);
            return request;
        };
    }

    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
