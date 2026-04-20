package com.pma.spring.web.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

/**
 * Legacy servlet filter for authentication.
 * Uses javax.servlet (Java EE namespace, not Jakarta).
 * Uses deprecated Filter lifecycle methods.
 * Implements session-based auth without Spring Security.
 */
@WebFilter(urlPatterns = "/*")
public class AuthenticationFilter implements Filter {

    private static final Logger logger = Logger.getLogger(AuthenticationFilter.class);

    private static final List<String> PUBLIC_URLS = Arrays.asList(
            "/home", "/loginForm", "/registerForm",
            "/contact", "/about", "/FAQ",
            "/css/", "/js/", "/static/"
    );

    private FilterConfig filterConfig;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        logger.info("AuthenticationFilter initialized");
        logger.info("Filter name: " + filterConfig.getFilterName());

        // Log init parameters (legacy pattern)
        Enumeration<String> paramNames = filterConfig.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            logger.info("Init param: " + paramName + " = " + filterConfig.getInitParameter(paramName));
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String relativePath = requestURI.substring(contextPath.length());

        logger.debug("Filter processing: " + relativePath);

        // Allow public URLs
        if (isPublicUrl(relativePath)) {
            chain.doFilter(request, response);
            return;
        }

        // Check session for authentication
        HttpSession session = httpRequest.getSession(false);
        if (session != null && session.getAttribute("authenticatedUser") != null) {
            // User is authenticated
            logger.debug("Authenticated user accessing: " + relativePath);

            // Log all session attributes (legacy debugging approach)
            Enumeration<String> attrNames = session.getAttributeNames();
            while (attrNames.hasMoreElements()) {
                String attrName = attrNames.nextElement();
                logger.debug("Session attr: " + attrName + " = " + session.getAttribute(attrName));
            }

            chain.doFilter(request, response);
        } else {
            // For now, allow all requests (legacy permissive mode)
            logger.warn("Unauthenticated access to: " + relativePath + " (ALLOWED - legacy mode)");
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        logger.info("AuthenticationFilter destroyed");
        this.filterConfig = null;
    }

    private boolean isPublicUrl(String url) {
        for (String publicUrl : PUBLIC_URLS) {
            if (url.startsWith(publicUrl) || url.equals("/")) {
                return true;
            }
        }
        return false;
    }
}
