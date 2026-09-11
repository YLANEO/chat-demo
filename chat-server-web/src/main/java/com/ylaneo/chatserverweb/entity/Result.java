package com.ylaneo.chatserverweb.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;//业务状态码  0-成功  1-业务逻辑错误（密码错误等） 2-系统错误（数据库连接出现问题等）
    private String message;//提示信息
    private T data;//响应数据

    //返回操作成功响应结果(带响应数据),第一个E表示这是一个支持泛型的函数，第二个E表示使用E这种类型去给前面定义的T data

    //操作成功，需要进行实体类数据返回的success，并且返回类型是 Result<E>,第三个E表示传入的参数类型是E
    public static <E> Result<E> success(E data) {
        return new Result<>(0, "操作成功", data);
    }

    //操作成功，不需要返回实体类数据的success，返回操作成功响应结果
    public static Result success() {
        return new Result(0, "操作成功", null);
    }

    //操作失败，返回失败（业务错误code=1）
    public static Result error(String message) {
        return new Result(1, message, null);
    }

    //操作失败，自定义业务code（用于GlobalExceptionHandler按异常类型返回不同code）
    public static Result error(Integer code, String message) {
        return new Result(code, message, null);
    }
}