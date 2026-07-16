package com.ylaneo.chatserver.util;


import java.util.ArrayList;
import java.util.List;

/**
 * 处理参数
 */
//用于解析client发送来的数据，解析出各个参数,而且不同的接口解析方式不一样？？，具体方法参考StringSplit类中的代码
public class RequestHandler {

    String request;//需要处理的数据流

    public List<String> handleParams(String request){//处理登录请求client传来的数据,并以List的方式进行数据的返回
        String str = request;
        String[] strlist = str.split("<");//借助正则表达式，进行字符串的分割，为什么是String类型？？？不都是流式数据和byte类型的数组吗？？？
        List<String> params=new ArrayList<>();
        System.out.println("解析出来的各个参数组合成的列表如下：");
        for(String a:strlist) {//将
            if(!a.isEmpty()){//第一个是空的，所以需要先排除第一个
                String b=a.substring(0,a.length()-1);
                params.add(b); //把解析出来的参数值都填写诶到数组中
                System.out.println(b);
            }
        }
        return params;
    }

    public static void main(String[] args) {//测试用的代码，可以删，暂时保留
        RequestHandler requestHandler=new RequestHandler();
        List<String> params=requestHandler.handleParams("<login><username><password>");
        System.out.println(params);
        for(int i=0;i<params.size();i++){
            System.out.println("params "+i+" is : "+params.get(i));
        }
    }

}
