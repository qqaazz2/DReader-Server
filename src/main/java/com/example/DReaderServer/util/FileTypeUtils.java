package com.example.DReaderServer.util;

import com.example.DReaderServer.common.BizException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FileTypeUtils {

    /**
     * 验证 MultipartFile
     *
     * @param file           要验证的 MultipartFile
     * @param allowedTypes   允许的文件类型（扩展名，例如：jpg, png, pdf）
     * @param maxSizeInBytes 最大文件大小（字节）
     * @return 验证结果，如果验证通过返回 null，否则返回错误信息
     */
    public static String validateFile(MultipartFile file, String[] allowedTypes, long maxSizeInBytes) {
        if (file == null || file.isEmpty()) {
            return "文件为空";
        }

        if (!isFileAllowedType(file, allowedTypes)) {
            return "文件类型不允许";
        }

        if (file.getSize() > maxSizeInBytes) {
            return "文件大小超过限制";
        }

        if (!isFileContentValid(file)) {
            return "文件内容无效";
        }

        return null; // 验证通过
    }

    /**
     * 检查 MultipartFile 类型是否允许
     *
     * @param file         要检查的 MultipartFile
     * @param allowedTypes 允许的文件类型（扩展名）
     * @return 如果文件类型允许返回 true，否则返回 false
     */
    private static boolean isFileAllowedType(MultipartFile file, String[] allowedTypes) {
        if (allowedTypes == null || allowedTypes.length == 0) {
            return true; // 如果没有指定允许的文件类型，则允许所有类型
        }

        String fileName = file.getOriginalFilename();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return false; // 没有扩展名
        }

        String fileType = fileName.substring(dotIndex + 1).toLowerCase();
        Set<String> allowedTypeSet = new HashSet<>(Arrays.asList(allowedTypes));
        return allowedTypeSet.contains(fileType);
    }

    /**
     * 检查 MultipartFile 内容是否有效
     *
     * @param file 要检查的 MultipartFile
     * @return 如果文件内容有效返回 true，否则返回 false
     */
    private static boolean isFileContentValid(MultipartFile file) {
        // 在这里实现文件内容验证逻辑，例如：
        // - 检查图片是否损坏
        // - 检查 PDF 文件是否可读
        // - 检查视频文件是否可播放
        // 示例：简单的文件头检查（仅适用于某些文件类型）
        try {
            byte[] fileContent = file.getBytes();

            if (fileContent.length > 4) {
                // 检查 PNG 文件头（示例）
                if (fileContent[0] == (byte) 0x89 && fileContent[1] == (byte) 0x50 &&
                        fileContent[2] == (byte) 0x4E && fileContent[3] == (byte) 0x47) {
                    return true;
                }
                // 检查 JPG 文件头（示例）
                if (fileContent[0] == (byte) 0xFF && fileContent[1] == (byte) 0xD8) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true; // 默认情况下，如果无法验证文件内容，则认为有效
    }
}