package com.ylaneo.chatserver.dao.impl;

import com.ylaneo.chatserver.dao.UserDao;
import com.ylaneo.chatserver.dto.RegisterDto;
import com.ylaneo.chatserver.entity.Result;
import com.ylaneo.chatserver.entity.User;
import com.ylaneo.chatserver.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDaoImpl implements UserDao {


    //注册，由于参数数量较多，修改为封装为对象的方式进行参数的传递
    @Override
    /*public Result register(String username, String password, String rePassword, Integer role) {
        //插入新数据到数据库的sql语句

        *//*mybatis用多了导致的
        String sql1="insert into user (username,password,role) values(#{username},#{password},#{role})";*//*

        //原生的jdbc要用问号进行占位
        //关于username唯一性检查直接交给数据库进行，因为建表的时候为username属性指定了unique
        String sql1="insert into user (username,password,role) values(?,?,?)";

        *//*查询所有username的语句，用于检查username是否重复
        String sql2="select username from user where username=?";*//*

        //检查两次输入的密码是否一致
        //if(password!=rePassword){  比较的是引用地址，不是字符串内容，永远返回 true。应该用 equals()。
        if(!password.equals(rePassword)){
            System.out.println("两次密码不一致");
            return Result.error("两次密码不一致");
        }
        //执行插入操作，并进行异常捕获
        try(Connection conn=new DBConnection().getConnection();
            PreparedStatement pt=conn.prepareStatement(sql1) ){
            //填入对应的数据
            pt.setString(1,username);
            pt.setString(2,password);
            //这里报错：为什么要求需要使用String类型？数据库中不是tinyint类型吗
            pt.setInt(3,role);
            //执行sql语句并处理查询到的数据
            int rows=pt.executeUpdate();
            if(rows>0){
                System.out.println("注册成功");
                return Result.success("注册成功");
            }
        }
        //异常处理
        catch (SQLException e){
            System.out.println("数据库操作错误"+e.getMessage());
            return Result.error(e.getMessage());
        }
        //其他异常问题
        return Result.error("注册失败");
    }*/
    public Result register(RegisterDto registerDto) {
        //从user对象中获取各个参数
        String username=registerDto.getUsername();
        String password=registerDto.getPassword();
        String rePassword=registerDto.getRePassword();
        Integer role=registerDto.getRole();

        /*mybatis用多了导致的
        String sql1="insert into user (username,password,role) values(#{username},#{password},#{role})";*/
        //原生的jdbc要用问号进行占位
        //关于username唯一性检查直接交给数据库进行，因为建表的时候为username属性指定了unique
        String sql1="insert into user (username,password,role) values(?,?,?)";

        /*查询所有username的语句，用于检查username是否重复
        String sql2="select username from user where username=?";*/

        //检查两次输入的密码是否一致
        //if(password!=rePassword){  比较的是引用地址，不是字符串内容，永远返回 true。应该用 equals()。
        if(!password.equals(rePassword)){
            System.out.println("两次密码不一致");
            return Result.error("两次密码不一致");
        }
        //执行插入操作，并进行异常捕获
        try(Connection conn=new DBConnection().getConnection();
            PreparedStatement pt=conn.prepareStatement(sql1) ){
            //填入对应的数据
            pt.setString(1,username);
            pt.setString(2,password);
            //这里报错：为什么要求需要使用String类型？数据库中不是tinyint类型吗
            pt.setInt(3,role);
            //执行sql语句并处理查询到的数据
            int rows=pt.executeUpdate();
            if(rows>0){
                System.out.println("注册成功");
                return Result.success("注册成功");
            }
        }
        //异常处理
        catch (SQLException e){
            System.out.println("数据库操作错误"+e.getMessage());
            return Result.error(e.getMessage());
        }
        //其他异常问题
        return Result.error("注册失败");
    }

    //登录
    @Override
    public Result login(String username, String password) {
        String sql="select * from user where username=?";
        //执行select操作，并进行异常捕获
        try(Connection conn=new DBConnection().getConnection();
            PreparedStatement pt=conn.prepareStatement(sql) ){
            //填入对应的数据
            pt.setString(1,username);
            //临时测试使用
            System.out.println("查询的username=[" + username + "], length=" + username.length());
            //执行sql语句并接受查询到的数据
            User user=new User();//创建一个User实例，传给登录页面进行用户信息状态管理
            ResultSet resultSet =pt.executeQuery();
            //这个要用.next()，而且这个方法的返回值是boolean类型的
            if(resultSet.next()){
//呃呃                if(resultSet.getString(password).equals(password)){
                if(resultSet.getString("password").equals(password)){
                    //为user实例赋值
                    user.setUserid(resultSet.getInt("id"));
                    user.setUsername(resultSet.getString("username"));
                    user.setRole(resultSet.getInt("role"));
                    //
                    Result<User>result=new Result();
                    System.out.println("登录成功");
                    return result.success(user);//返回查找到的信息给
                }
                else{
                    return Result.error("密码错误");
                }
            }
            else{
                return Result.error("用户不存在");
            }
        }
        //异常处理
        catch (SQLException e){
            System.out.println("数据库操作错误"+e.getMessage());
            return Result.error(e.getMessage());
        }
    }
    //修改用户名
    @Override
    public Result updateUsername(Integer userId, String reUsername) {
        String sql="update user set username=? where id=?";
        try(Connection conn=new DBConnection().getConnection();
            PreparedStatement pt=conn.prepareStatement(sql);){
            //
            pt.setString(1,reUsername);
            pt.setInt(2,userId);
            //
            Result result=new Result<>();
            int rows=pt.executeUpdate();
            if(rows!=0){
                return result.success("修改成功");
            }else {
                return result.error("修改失败");
            }
        }catch (SQLException e){
            return Result.error(e.getMessage());
        }
    }
    //获得当前用户的username
    @Override
    public Result getUsername(Integer userId) {
        String sql="select * from user where id=?";
        try(Connection conn=new DBConnection().getConnection();
            PreparedStatement pt=conn.prepareStatement(sql);){
            //
            pt.setInt(1,userId);
            //
            Result result=new Result<>();
            ResultSet resultSet=pt.executeQuery();
            if(resultSet.next()){
                User user=new User();
                user.setUsername(resultSet.getString("username"));
                return result.success(user);
            }else {
                return result.error("查询失败");
            }
        }catch (SQLException e){
            return Result.error(e.getMessage());
        }
    }

}
