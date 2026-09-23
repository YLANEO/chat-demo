package com.ylaneo.chatserverweb.exception;

import com.ylaneo.chatserverweb.entity.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {


    //404表示：路径不存在/静态资源找不到，静态资源404改成了抛NoResourceFoundException，controller路径找不到抛NoHandlerFoundException。
    // 这两个会被Exception.class的handler截胡，必须单独声明。
    @ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
    public ResponseEntity<Result> handleNotFound(Exception e) {
        //不打印stacktrace，因为这是正常的"找不到"，不是bug
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Result.error(1, StringUtils.hasLength(e.getMessage()) ? e.getMessage() : "资源不存在"));//借助Result类进行异常信息的返回
    }

    //400：参数错误/业务参数校验
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result> handleBadRequest(IllegalArgumentException e) {
        e.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.error(1, StringUtils.hasLength(e.getMessage()) ? e.getMessage() : "参数错误"));//借助Result类进行异常信息的返回
    }


    //其他错误用500：其它未预期异常（系统错误）。兜底用的。
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleException(Exception e) {
        e.printStackTrace();
        String msg = StringUtils.hasLength(e.getMessage()) ? e.getMessage() : "操作失败";
        //code=2区分系统错误，方便前端按code区分业务错误与系统错误
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(2, msg));//借助Result类进行异常信息的返回
    }
}
