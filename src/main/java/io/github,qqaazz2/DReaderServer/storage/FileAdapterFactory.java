package io.github.qqaazz2.DReaderServer.storage;

import io.github.qqaazz2.DReaderServer.common.BizException;
import io.github.qqaazz2.DReaderServer.entity.LoginUser;
import io.github.qqaazz2.DReaderServer.enums.ExceptionEnum;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FileAdapterFactory {
    @Resource
    RedisTemplate redisTemplate;

    Map<String, FileAdapterService> adapterServiceMap = new HashMap<>();
    volatile String fileAdapterType = "system";

    public FileAdapterFactory(List<FileAdapterService> list) {
        for (FileAdapterService fileAdapterService : list) {
            adapterServiceMap.put(fileAdapterService.getStorageType(), fileAdapterService);
        }
    }

    public FileAdapterService getFileAdapter(String name) {
        FileAdapterService fileAdapterService = adapterServiceMap.get(name);
        if (fileAdapterService == null) throw new BizException("4000", "未找到对应的文件适配器");
        return fileAdapterService;
    }

    public FileAdapterService getFileAdapter() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) throw new BizException(ExceptionEnum.INTERNAL_SERVER_ERROR);
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String type = loginUser.getUser().getFileAdapter();
        FileAdapterService service = adapterServiceMap.get(type);
        if (service == null) throw new BizException("4000", "未找到对应的文件适配器");
        return service;
    }

    public List<String> getAdapterTypeList() {
        return adapterServiceMap.keySet().stream().collect(Collectors.toList());
    }
}
