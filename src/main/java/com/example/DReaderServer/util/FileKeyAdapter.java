package com.example.DReaderServer.util;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinBase.FILETIME;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.W32APIOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;

public class FileKeyAdapter {
    public static String getFileKey(File file) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return getFileKeyWindows(file);
        } else {
            return getFileKeyUnix(file);
        }
    }

    private static String getFileKeyUnix(File file) throws IOException {
        Path path = file.toPath();
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        Object fileKey = attrs.fileKey();
        return fileKey != null ? fileKey.toString() : null;
    }

    private static String getFileKeyWindows(File file) throws IOException {
        HANDLE hFile = Kernel32.INSTANCE.CreateFile(
                file.getAbsolutePath(),
                WinNT.GENERIC_READ,
                WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE | WinNT.FILE_SHARE_DELETE,
                null,
                WinNT.OPEN_EXISTING,
                WinNT.FILE_ATTRIBUTE_NORMAL,
                null
        );

        if (WinBase.INVALID_HANDLE_VALUE.equals(hFile)) {
            throw new IOException("找不到文件: " + file.getAbsolutePath());
        }

        try {
            BY_HANDLE_FILE_INFORMATION fileInfo = new BY_HANDLE_FILE_INFORMATION();
            if (!ExtendedKernel32.INSTANCE.GetFileInformationByHandle(hFile, fileInfo)) {
                throw new IOException("GetFileInformationByHandle 调用失败，文件路径为: " + file.getAbsolutePath());
            }

            long fileIndex = ((fileInfo.nFileIndexHigh.longValue()) << 32) | (fileInfo.nFileIndexLow.longValue() & 0xffffffffL);
            return String.valueOf(fileIndex);
        } finally {
            Kernel32.INSTANCE.CloseHandle(hFile);
        }
    }

    public static class BY_HANDLE_FILE_INFORMATION extends Structure {
        public DWORD dwFileAttributes;
        public FILETIME ftCreationTime;
        public FILETIME ftLastAccessTime;
        public FILETIME ftLastWriteTime;
        public DWORD dwVolumeSerialNumber;
        public DWORD nFileSizeHigh;
        public DWORD nFileSizeLow;
        public DWORD nNumberOfLinks;
        public DWORD nFileIndexHigh;
        public DWORD nFileIndexLow;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "dwFileAttributes", "ftCreationTime", "ftLastAccessTime", "ftLastWriteTime",
                    "dwVolumeSerialNumber", "nFileSizeHigh", "nFileSizeLow", "nNumberOfLinks",
                    "nFileIndexHigh", "nFileIndexLow"
            );
        }
    }

    public interface ExtendedKernel32 extends Kernel32 {
        ExtendedKernel32 INSTANCE = Native.load("kernel32", ExtendedKernel32.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean GetFileInformationByHandle(WinNT.HANDLE hFile, BY_HANDLE_FILE_INFORMATION lpFileInformation);
    }
}