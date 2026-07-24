package com.ylaneo.chatclientdesktop.util;


import java.util.ArrayList;
import java.util.List;

//用于解析server发送来的响应数据，解析出各个参数,具体方法参考StringSplit类中的代码
public class ResponseHandler {

    String response;//需要处理的数据流

    public List<String> handleParams(String response){//处理登录请求client传来的数据,并以List的方式进行数据的返回
        String str = response;
        String[] strlist = str.split("<");//借助正则表达式，进行字符串的分割，为什么是String类型？？？不都是流式数据和byte类型的数组吗？？？
        List<String> result=new ArrayList<>();
        for(String a:strlist) {//将
            if(!a.isEmpty()){//第一个是空的，所以需要先排除第一个
                String b=a.substring(0,a.length()-1);
                result.add(b); //把解析出来的参数值都填写诶到数组中
                System.out.println(b);
            }
        }
        return result;//返回解析出的列表类型的数据
    }

//    public static void main(String[] args) {//测试用的代码，可以删，暂时保留
//        ResponseHandler responseHandler=new ResponseHandler();
//        List<String> params=responseHandler.handleParams("<login><username><password>");
//        for(int i=0;i<params.size();i++){
//            System.out.println("params "+i+" is : "+params.get(i));
//        }
//    }

}
