package capstone.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.*;

public class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
    private final Map<String, String> customHeaders = new HashMap<>();

    public HeaderMapRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    public HeaderMapRequestWrapper addHeader(String name, String value) {
        customHeaders.put(name, value);
        return this;
    }

    @Override
    public String getHeader(String name) {
        String h = customHeaders.get(name);
        return (h != null) ? h : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String h = customHeaders.get(name);
        if (h != null) {
            return Collections.enumeration(List.of(h));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new HashSet<>(customHeaders.keySet());
        Enumeration<String> e = super.getHeaderNames();
        while (e.hasMoreElements()) names.add(e.nextElement());
        return Collections.enumeration(names);
    }
}