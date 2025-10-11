package capstone.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    public String getUserId(HttpServletRequest req) {
        String h = req.getHeader("X-Demo-UserId");
        return (h != null && !h.isBlank()) ? h : "anonymous";
    }
}
