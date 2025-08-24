package com.example.DReaderServer.common;

import com.example.DReaderServer.enums.ExceptionEnum;
import lombok.Data;

@Data
public class ResultResponse {
    private String code;
    private String message;
    private Object result;

    public ResultResponse() {
    }

    public ResultResponse(BaseErrorInfoInterface errorInfoInterface) {
        this.code = errorInfoInterface.getResultCode();
        this.message = errorInfoInterface.getResultMsg();
    }

    public static ResultResponse success() {
        return success(null);
    }

    public static ResultResponse success(Object object) {
        ResultResponse resultResponse = new ResultResponse();
        resultResponse.setMessage(ExceptionEnum.SUCCESS.getResultMsg());
        resultResponse.setCode(ExceptionEnum.SUCCESS.getResultCode());
        resultResponse.setResult(object);
        return resultResponse;
    }

    public static ResultResponse success(Object object,String msg) {
        ResultResponse resultResponse = new ResultResponse();
        resultResponse.setMessage(msg);
        resultResponse.setCode(ExceptionEnum.SUCCESS.getResultCode());
        resultResponse.setResult(object);
        return resultResponse;
    }

    public static ResultResponse error(BaseErrorInfoInterface baseErrorInfoInterface) {
        ResultResponse resultResponse = new ResultResponse(baseErrorInfoInterface);
        return resultResponse;
    }

    public static ResultResponse error(String errorCode, String errorMsg) {
        ResultResponse resultResponse = new ResultResponse();
        resultResponse.setCode(errorCode);
        resultResponse.setMessage(errorMsg);
        return resultResponse;
    }

    public static ResultResponse error(String errorMsg) {
        ResultResponse resultResponse = new ResultResponse();
        resultResponse.setCode("-1");
        resultResponse.setMessage(errorMsg);
        return resultResponse;
    }
}
