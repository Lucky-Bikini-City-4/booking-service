package com.dayaeyak.booking.interceptor;

import com.dayaeyak.booking.annotation.Authorize;
import com.dayaeyak.booking.common.enums.UserRole;
import com.dayaeyak.booking.common.exception.CustomException;
import com.dayaeyak.booking.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;

@Slf4j
@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Authorize authorize = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Authorize.class); // method level

        if (authorize == null) {
            authorize = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Authorize.class); // class level
        }

        // 없으면 모든 권한 가능
        if (authorize == null) {
            return true;
        }

        // 명시 목적으로 bypass == true면 모든 권한 가능
        if (authorize.bypass()) {
            return true;
        }

        // 권한 목록이 비었으면 모든 권한 가능
        if (authorize.roles().length == 0) {
            return true;
        }

        String role = request.getHeader(USER_ROLE_HEADER);

        if (!StringUtils.hasText(role)) {
            throw new CustomException(ErrorCode.INVALID_USER_ROLE);
        }

        UserRole userRole = UserRole.of(role);

        boolean isExists = Arrays.asList(authorize.roles())
                .contains(userRole);

        if (!isExists) {
            throw new CustomException(ErrorCode.REQUEST_ACCESS_DENIED);
        }

        return true;
    }
}