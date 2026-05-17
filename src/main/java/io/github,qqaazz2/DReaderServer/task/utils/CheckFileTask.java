package io.github.qqaazz2.DReaderServer.task.utils;

import io.github.qqaazz2.DReaderServer.common.TaskInterruptedException;
import io.github.qqaazz2.DReaderServer.entity.files.Files;
import io.github.qqaazz2.DReaderServer.enums.FilesCheckType;
import io.github.qqaazz2.DReaderServer.util.FileKeyAdapter;
import io.github.qqaazz2.DReaderServer.util.FilesUtils;
import lombok.AllArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@AllArgsConstructor
public class CheckFileTask implements Callable<FileTaskResult> {
    private FilesUtils filesUtils;
    private File file;
    private Long parentID;
    private int order;
    private ScanContext scanContext;

    //1.如果判断inode、hash完全一致，则检查是否需要修改path、name、parentId;
    //2.如果判断hash、parentId、size一致并且有且仅有一个文件，修改path、name、inode
    @Override
    public FileTaskResult call() throws Exception {
        //CREATE为新增 RENAME为重命名 NORMAL为正常（数据库中已经有数据不需要新增或重命名） SPACIAL暂时无法判断类型
        FilesCheckType filesType = FilesCheckType.NORMAL;
        Files files;
        if (Thread.currentThread().isInterrupted()) throw new TaskInterruptedException();
        String hash = filesUtils.getFileChecksum(file);
        String inode = FileKeyAdapter.getFileKey(file);
        List<Files> filesList = scanContext.getFilesList().stream().filter(item -> item.getHash().equals(hash)).collect(Collectors.toList());
        if (filesList.isEmpty()) {
            filesType = FilesCheckType.CREATE;
            files = filesUtils.createFiles(file, parentID, order, inode);
            return new FileTaskResult(filesType, files);
        }

        for (Files hashSameFiles : filesList) {
            if (hashSameFiles.getInode().equals(inode)) {
                if (!hashSameFiles.getParentId().equals(parentID) || !hashSameFiles.getFilePath().equals(file.getPath()) || !hashSameFiles.getFileName().equals(file.getName())) {
                    filesType = FilesCheckType.RENAME;
                    hashSameFiles.setFileName(file.getName());
                    hashSameFiles.setFilePath(file.getPath());
                    hashSameFiles.setParentId(parentID);
                }
                return new FileTaskResult(filesType, hashSameFiles);
            }
        }

        if(filesList.size() == 1 && filesList.get(0).getParentId().equals( parentID) && filesList.get(0).getFileSize().equals(file.length()) && ((!filesList.get(0).getParentId().equals(parentID) || !filesList.get(0).getFilePath().equals(file.getPath()) || !filesList.get(0).getFileName().equals(file.getName())) )){
            filesType = FilesCheckType.RENAME;
            files = filesList.get(0);
            files.setFileName(file.getName());
            files.setFilePath(file.getPath());
            files.setInode(inode);
            return new FileTaskResult(filesType, files);
        }

        filesType = FilesCheckType.CREATE;
        files = filesUtils.createFiles(file, parentID, order, inode);
        return new FileTaskResult(filesType, files);
    }
}