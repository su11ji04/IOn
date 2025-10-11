package capstone.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile("demo")
@ConditionalOnProperty(prefix="demo", name="enabled", havingValue="true", matchIfMissing=false)
@RequiredArgsConstructor
public class DemoUserFilter extends OncePerRequestFilter {

    private final DemoProps props;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws java.io.IOException, jakarta.servlet.ServletException {

        if (req.getHeader("X-Demo-UserId") == null) {

            chain.doFilter(new HeaderMapRequestWrapper(req)
                    .addHeader("X-Demo-UserId", props.getUserId()), res);
            return;
        }
        chain.doFilter(req, res);
    }
}