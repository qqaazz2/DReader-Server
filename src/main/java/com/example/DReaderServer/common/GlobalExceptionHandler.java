package com.example.DReaderServer.common;

import com.example.DReaderServer.enums.ExceptionEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.naming.AuthenticationException;
import java.util.List;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = BizException.class)
    @ResponseBody
    public ResultResponse bizExceptionHandler(HttpServletRequest servletRequest,BizException bizException){
        log.error(bizException.getErrorMsg());
        return ResultResponse.error(bizException.getErrorCode(),bizException.getErrorMsg());
    }

    @ExceptionHandler(value = NullPointerException.class)
    @ResponseBody
    public ResultResponse exceptionHandler(HttpServletRequest servletRequest,NullPointerException exception){
        exception.printStackTrace();
        log.error(exception.getMessage());
        return ResultResponse.error(ExceptionEnum.BODY_NOT_MATCH);
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ResultResponse exceptionHandler(HttpServletRequest req, Exception e){
        e.printStackTrace();
        log.error(e.getMessage());
        return ResultResponse.error(ExceptionEnum.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    @ResponseBody
    public ResultResponse exceptionHandler(HttpServletRequest req, MissingServletRequestParameterException e){
        log.error(e.getMessage());
        return ResultResponse.error(ExceptionEnum.BODY_NOT_MATCH);
    }

    @ExceptionHandler(value = AuthenticationException.class)
    @ResponseBody
    public ResultResponse exceptionHandler(HttpServletRequest req, AuthenticationException e){
        log.error(e.getMessage());
        return ResultResponse.error(ExceptionEnum.NOT_AUTHORITY);
    }

    @ExceptionHandler(value = BadCredentialsException.class)
    @ResponseBody
    public ResultResponse exceptionHandler(HttpServletRequest req, BadCredentialsException e){
        log.error("账号或密码错误");
        return ResultResponse.error("4000","账号或密码错误");
    }

    @ExceptionHandler(value = InternalAuthenticationServiceException.class)
    @ResponseBody
    public ResultResponse exceptionHandler(HttpServletRequest req, InternalAuthenticationServiceException e){
        log.error(e.getMessage());
        return ResultResponse.error("4000",e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        StringBuilder errorMsg = new StringBuilder();
        for (ObjectError error : allErrors) {
            errorMsg.append(error.getDefaultMessage()).append("; ");
        }
        log.error(errorMsg.toString());
        return ResultResponse.error("4000",errorMsg.toString());
    }
}
