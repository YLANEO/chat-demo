package com.ylaneo.chatserverweb.exception;

import com.ylaneo.chatserverweb.entity.Result;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    //Result仍然是通用的返回类型
    public Result handleException(Exception e)
    {
        //先输出到后台
        e.printStackTrace();
        //返回给前端错误信息
        //return Result.error(e.getMessage());
        //不用前面那个的原因是不确定e.getMessage()是否有返回信息，为了保证健壮性先进行判断，如果没有返回则自己添加返回信息
        return Result.error(StringUtils.hasLength(e.getMessage())?e.getMessage():"操作失败");

    }

}
