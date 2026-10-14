package io.github.qqaazz2.DReaderServer.handle;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public abstract class JsonListTypeHandler<T>
        extends AbstractJsonTypeHandler<List<T>> {

    private final TypeReference<List<T>> type;

    protected JsonListTypeHandler(TypeReference<List<T>> type) {
        this.type = type;
    }

    @Override
    protected List<T> parse(String json) {
        try {
            return new ObjectMapper()
                    .readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected String toJson(List<T> obj) {
        try {
            return new ObjectMapper()
                    .writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}