package com.ruoyi.framework.security.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysUserService;

/**
 * token过滤器 验证token有效性
 * 
 * @author ruoyi
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter
{
    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysUserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNull(SecurityUtils.getAuthentication()))
        {
            tokenService.verifyToken(loginUser);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        if (requiresAgreement(loginUser, request))
        {
            ServletUtils.renderString(response, JSON.toJSONString(
                    AjaxResult.error(HttpStatus.FORBIDDEN, "请先签署保密协议后再使用系统")));
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean requiresAgreement(LoginUser loginUser, HttpServletRequest request)
    {
        if (StringUtils.isNull(loginUser) || !isIntern(loginUser.getUser()))
        {
            return false;
        }
        if (Integer.valueOf(1).equals(loginUser.getUser().getProtocolStatus()))
        {
            return false;
        }
        SysUser freshUser = userService.selectUserById(loginUser.getUserId());
        if (freshUser != null && Integer.valueOf(1).equals(freshUser.getProtocolStatus()))
        {
            loginUser.getUser().setProtocolStatus(1);
            tokenService.setLoginUser(loginUser);
            return false;
        }
        String uri = request.getRequestURI();
        return !("OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/getInfo".equals(uri)
                || "/getRouters".equals(uri)
                || "/logout".equals(uri)
                || "/business/auth/agreement".equals(uri)
                || "/business/auth/agreement/sign".equals(uri));
    }

    private boolean isIntern(SysUser user)
    {
        if (user == null || user.getRoles() == null)
        {
            return false;
        }
        for (SysRole role : user.getRoles())
        {
            if ("PRE_TRAINEE".equals(role.getRoleKey()) || "FORMAL_TRAINEE".equals(role.getRoleKey()))
            {
                return true;
            }
        }
        return false;
    }
}
