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

    /**
     * 允许大文件（course-max-request-size）的 multipart 端点白名单。
     * 课程资料接口自 2026-09-23 起上限 3300MB（文档类附件放开到 3GB）；其余接口仍是 20MB 档。
     *
     * <p>⚠️ 新增「能传大文件」的上传接口时<b>必须</b>在这里加一条，且与 Controller 的
     * {@code @PostMapping} 路径逐字一致；否则该接口会被默认 20MB 档静默卡成 413，
     * 而错误信息完全不指向这里。</p><p>
     * ⚠️ 生产环境若前面挂了反向代理（nginx），还要同步 {@code client_max_body_size} 与
     * 超时（默认 1MB 会直接把大文件挡在代理层）。</p>
     */
    private static final String[] LARGE_MULTIPART_PATTERNS = {
            "/business/course/items/\\d+/asset",
            "/business/task-attachment/upload",
            "/business/task-attachment/upload-submission"
    };

    @Value("${upload.default-max-request-size:20MB}")
    @DataSizeUnit(DataUnit.MEGABYTES)
    private DataSize defaultMaxRequestSize;

    @Value("${upload.course-max-request-size:3300MB}")
    @DataSizeUnit(DataUnit.MEGABYTES)
    private DataSize courseMaxRequestSize;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String contentType = request.getContentType();
        long contentLength = request.getContentLengthLong();
        if (contentType != null && contentType.toLowerCase().startsWith(MULTIPART_PREFIX) && contentLength >= 0) {
            String uri = request.getRequestURI().substring(request.getContextPath().length());
            boolean large = false;
            for (String pattern : LARGE_MULTIPART_PATTERNS)
            {
                if (uri.matches(pattern))
                {
                    large = true;
                    break;
                }
            }
            long limit = large ? courseMaxRequestSize.toBytes() : defaultMaxRequestSize.toBytes();
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
