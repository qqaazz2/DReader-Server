package io.github.qqaazz2.DReaderServer.enums;

public enum FilesMatchType {
    EXACT,   // hash + path 相同
    HASH,    // hash 相同
    PATH,    // path 相同
    NONE     // 都不相同
}
