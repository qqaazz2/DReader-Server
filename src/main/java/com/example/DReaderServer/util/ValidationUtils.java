package com.example.DReaderServer.util;

import com.example.DReaderServer.common.BizException;


public class ValidationUtils {
    private ValidationUtils(){};

    public static void checkCondition(boolean condition,String msg)  {
        if(!condition) throw new BizException("4000",msg);
    }

    public static void checkCondition(boolean condition,String code,String msg)  {
        if(!condition) throw new BizException(code,msg);
    }
}
