package com.ylaneo.chatclientdesktop.view.dialogs;

import javax.swing.*;
import java.awt.*;

public class EditDialogView extends JDialog {
    //组件定义和初始化
    JTextField jTextFieldBookname=new JTextField(30);
    JTextField jTextFieldAuthor=new JTextField(30);
    JTextField jTextFieldCount=new JTextField(30);
    JButton jButtonConfirm=new JButton("确认");
    JButton jButtonCancel=new JButton("取消");
    boolean confirmed=false;//标志位
    //
    public EditDialogView(Frame parent, String title){
        //构造函数,JDialog(Frame owner, String title, boolean modal) ,只能用Frame，因此bookmanageview中还需要向上找到他的Jframe类型的父类Mainview
//        super(parent,"新增书",true);
        super(parent,title,true);
//        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        setLayout(new GridLayout(4,1,10,10));
//        setSize(400,400);
        setLocationRelativeTo(null);
        //
//        this.setVisible(true);

        
        //
        JPanel row1=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel row2=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel row3=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel row4=new JPanel(new FlowLayout());
        //
        JLabel label1 = new JLabel("书名:");
        label1.setPreferredSize(new Dimension(70, 25));//设置统一的宽度进行对齐
        row1.add(label1);
        row1.add(jTextFieldBookname);

        JLabel label2 = new JLabel("作者:");
        label2.setPreferredSize(new Dimension(70, 25));//设置统一的宽度进行对齐
        row2.add(label2);
        row2.add(jTextFieldAuthor);

        JLabel label3 = new JLabel("库存数量:");
        label3.setPreferredSize(new Dimension(70, 25));//设置统一的宽度进行对齐
        row3.add(label3);
        row3.add(jTextFieldCount);

        row4.add(jButtonConfirm);
        row4.add(jButtonCancel);
        //
        this.add(row1);
        this.add(row2);
        this.add(row3);
        this.add(row4);

        // ======================================== 按钮功能 ========================================
        //确定按钮
        jButtonConfirm.addActionListener(e -> {

            //原来的版本：没有对这个页面进行复用，确定按钮中直接包含了添加书的逻辑
            /*//
            BookDao bookDao=new BookDaoImpl();
            String bookname=jTextFieldBookname.getText().trim();//trim函数可以清除文本中的空格内容
            String author=jTextFieldAuthor.getText();
//           Integer count=(Integer)jTextFieldCount.getText();//不能这么转
            Integer count = Integer.parseInt(jTextFieldCount.getText());//对的对的
            //兜底写法:如果输入的不是有效数字（比如空字符串或字母），parseInt 会抛出 NumberFormatException。加个try-catch兜底：
            *//*Result result=new Result<>();
            try {
                Integer count = Integer.parseInt(jTextFieldCount.getText());
                result = bookDao.addBook(bookname, author, count);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "库存数量请输入数字");
            }*//*
            Result result=bookDao.addBook(bookname,author,count);//调用接口
            if(result.getCode()==0){//成功
                confirmed=true;//修改标志位
                JOptionPane.showMessageDialog(this,result.getMessage());
                this.dispose();
            }else{
                JOptionPane.showMessageDialog(this,result.getMessage());
            }*/

            //新版：只修改标志位,接口调用逻辑交给管理页面去做
            confirmed=true;
            dispose();
        });

        jButtonCancel.addActionListener(e->{
            this.dispose();
        });

        // ======================================== 注意这两行的位置，放错位置了会g ========================================
        pack();// pack() 根据组件内容自动计算窗口大小（关键！）
//        setVisible(true);//这个需要在pack执行之后，不然没有组件就直接显示出来了
    }

    //判断在这个窗口中的操作是否成功，根据标志位来判断
    public boolean isConfirmed(){
        return confirmed;
    }

    //自动填充数据的函数
    public void autoFillFields(String bookname,String author,String count){
        jTextFieldBookname.setText(bookname);
        jTextFieldAuthor.setText(author);
        jTextFieldCount.setText(count);
    }

    //返回本窗口中的三个文本框中的内容的函数
    public String getBookname(){
        return jTextFieldBookname.getText();
    }
    public String getAuthor(){
        return jTextFieldAuthor.getText();
    }
    public String getCount(){
        return jTextFieldCount.getText();
    }
}
