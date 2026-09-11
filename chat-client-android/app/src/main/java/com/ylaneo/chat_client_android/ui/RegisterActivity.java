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
import com.ylaneo.chat_client_android.R;
import com.ylaneo.chat_client_android.manager.UserManager;

/**
 * 注册页面
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private TextInputEditText etRepassword;
    private MaterialButton btnRegister;
    private MaterialButton btnGoLogin;
    private View progressRegister;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        initListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etRepassword = findViewById(R.id.et_repassword);
        btnRegister = findViewById(R.id.btn_register);
        btnGoLogin = findViewById(R.id.btn_go_login);
        progressRegister = findViewById(R.id.progress_register);
    }

    private void initListeners() {
        btnRegister.setOnClickListener(v -> handleRegister());
        btnGoLogin.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String repassword = etRepassword.getText() != null ? etRepassword.getText().toString() : "";

        if (username.isEmpty() || password.isEmpty() || repassword.isEmpty()) {
            Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(repassword)) {
            Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                UserManager.getInstance().register(username, password, repassword);
                mainHandler.post(() -> {
                    setLoading(false);
                    Toast.makeText(this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
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
        btnRegister.setEnabled(!loading);
        btnGoLogin.setEnabled(!loading);
        progressRegister.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
