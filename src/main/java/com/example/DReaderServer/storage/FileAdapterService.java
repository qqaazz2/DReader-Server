package com.example.DReaderServer.storage;

import java.io.File;
import java.util.List;
import java.util.Set;

public abstract class FileAdapterService {
   public abstract String upload(File file);

    public abstract String upload(byte[] data, String fileName, String contentType);

    public abstract String uploadSplicing(byte[] data, String fileName, String contentType);

    public abstract String getUrl(String fileName);

    public abstract String getStorageType();

    public abstract Set<String> getFileList(String path);

    public abstract void removeByList(Set<String> names);
}
