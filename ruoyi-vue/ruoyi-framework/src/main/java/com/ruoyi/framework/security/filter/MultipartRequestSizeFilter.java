package com.ruoyi.framework.security.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import org.springframework.web.filter.OncePerRequestFilter;

/** Limits large multipart requests to the dedicated course asset endpoint. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MultipartRequestSizeFilter extends OncePerRequestFilter {

    private static final String MULTIPART_PREFIX = "multipart/";
    private static final String COURSE_ASSET_PATTERN = "/business/course/items/\\d+/asset";

    @Value("${upload.default-max-request-size:20MB}")
    @DataSizeUnit(DataUnit.MEGABYTES)
    private DataSize defaultMaxRequestSize;

    @Value("${upload.course-max-request-size:510MB}")
    @DataSizeUnit(DataUnit.MEGABYTES)
    private DataSize courseMaxRequestSize;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String contentType = request.getContentType();
        long contentLength = request.getContentLengthLong();
        if (contentType != null && contentType.toLowerCase().startsWith(MULTIPART_PREFIX) && contentLength >= 0) {
            String uri = request.getRequestURI().substring(request.getContextPath().length());
            long limit = uri.matches(COURSE_ASSET_PATTERN)
                    ? courseMaxRequestSize.toBytes() : defaultMaxRequestSize.toBytes();
            if (contentLength > limit) {
                response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":413,\"msg\":\"上传请求超过允许大小\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
