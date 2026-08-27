package com.ylaneo.chatserverweb.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    //进行数据库连接配置，可以加上final关键字，因为确认这几个变量不会更改了
    private static  String url = "jdbc:mysql://localhost:3306/chat_demo?serverTimezone=UTC";
    private static  String user = "root";
    private static  String password = "123456";
    private static  String driver = "com.mysql.cj.jdbc.Driver";
    //数据库连接方法
    public Connection getConnection(){
        //数据库了解对象
        Connection conn=null;
        try{
            //直接使用上面定义的数据库驱动名称
            Class.forName(driver);
            //进行连接
            conn= DriverManager.getConnection(url,user,password);
            //数据库连接成功
            System.out.println("数据库连接成功"+conn);
        }
        //进行异常捕获1
        catch (ClassNotFoundException e){
            System.out.println("数据库连接驱动出错");
            e.printStackTrace();
        }
        //异常捕获2
        catch (SQLException e){
            System.out.println("数据库连接失败(参数出错)或者sql语句语法出错");
            e.printStackTrace();
        }
        //返回数据库连接对象
        return conn;
    }
    //关闭数据库连接
    public void close(Connection connection){
        if(connection!=null){
            try{
                connection.close();
                System.out.println("数据库关闭成功");
            }
            catch (SQLException e){
                e.printStackTrace();
            }
        }
    }
}
