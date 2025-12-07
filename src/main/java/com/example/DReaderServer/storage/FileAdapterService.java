package com.example.DReaderServer.storage;

import java.io.File;

public abstract class FileAdapterService {
   public abstract String upload(File file);

    public abstract String upload(byte[] data, String fileName, String contentType);

    public abstract String getUrl(String fileName);

    public abstract String getStorageType();
}
