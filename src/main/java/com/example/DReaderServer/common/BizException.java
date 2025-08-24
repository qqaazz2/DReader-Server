package com.example.DReaderServer.common;

import lombok.Data;

@Data
//自定义异常类
public class BizException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    protected String errorMsg;
    protected String errorCode;

    public BizException(){
        super();
    }

    public BizException(BaseErrorInfoInterface errorInfoInterface){
        super(errorInfoInterface.getResultCode());
        this.errorCode = errorInfoInterface.getResultCode();
        this.errorMsg = errorInfoInterface.getResultMsg();
    }

    public BizException(BaseErrorInfoInterface errorInfoInterface,Throwable throwable){
        super(errorInfoInterface.getResultCode(),throwable);
        this.errorCode = errorInfoInterface.getResultCode();
        this.errorMsg = errorInfoInterface.getResultMsg();
    }

    public BizException(String errorMsg){
        super(errorMsg);
        this.errorMsg = errorMsg;
    }

    public BizException(String errorCode,String errorMsg){
        super(errorCode);
        this.errorMsg = errorMsg;
        this.errorCode = errorCode;
    }

    public BizException(String errorCode,String errorMsg,Throwable throwable){
        super(errorCode,throwable);
        this.errorMsg = errorMsg;
        this.errorCode = errorCode;
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
