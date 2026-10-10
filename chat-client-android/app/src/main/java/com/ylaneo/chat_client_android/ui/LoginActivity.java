package com.ylaneo.chat_client_android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;


import com.ylaneo.chat_client_android.MainActivity;
import com.ylaneo.chat_client_android.R;
import com.ylaneo.chat_client_android.manager.UserManager;
import com.ylaneo.chat_client_android.model.User;
import com.ylaneo.chat_client_android.network.PushManager;
import com.ylaneo.chat_client_android.session.UserSession;

/**
 * 登录页面
 *
 * Android UI 操作必须在主线程，网络请求在子线程。
 * 做法：用 new Thread + Handler(Looper.getMainLooper()) 把结果切回主线程。
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnGoRegister;
    private View progressLogin;

    //持有Handler(Looper.getMainLooper())在任何地方切回主线程的通用手段,因为android中的UI不是线程安全的，处理ui变化需要使用主线程
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        initListeners();
    }

    //初始化组件，为各个定义的变量绑定对应的组件（在layout中的）
    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoRegister = findViewById(R.id.btn_go_register);
        progressLogin = findViewById(R.id.progress_login);
    }

    //为按钮绑定对应的功能
    private void initListeners() {
        // 登录按钮
        btnLogin.setOnClickListener(v -> handleLogin());

        // 去注册
        btnGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    //登录按钮点击后的逻辑
    private void handleLogin() {
        //判断为空？
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                User user = UserManager.getInstance().login(username, password);

                // 保存登录状态
                UserSession.getInstance().login(user);

                // 建立推送长连接
                PushManager.getInstance().connect(user.getUserid());

                // 更新在线状态
                UserManager.getInstance().updateUserStatus(user.getUserid(), 1);

                //使用主线程处理UI变化，UI不是线程安全的
                mainHandler.post(() -> {
                    setLoading(false);
                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
                    // 跳转到主界面
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnGoRegister.setEnabled(!loading);
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
