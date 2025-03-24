package org.zhou.backend.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.zhou.backend.security.UserPrincipal;

public class SecurityUtils {
    public static String getCurrentUserId() {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();
        return userPrincipal.getId().toString();
    }
} 