package io.github.qqaazz2.DReaderServer.common;

import io.github.qqaazz2.DReaderServer.entity.LoginUser;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
public class CustomPermissionEvaluator implements PermissionEvaluator {
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        authentication = SecurityContextHolder.getContext().getAuthentication();

        LoginUser userDetails = (LoginUser) authentication.getPrincipal();
        return userDetails != null && userDetails.getUser().getMystery().equals(permission);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }
}
