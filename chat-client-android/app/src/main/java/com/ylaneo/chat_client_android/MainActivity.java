package com.ylaneo.chat_client_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ylaneo.chat_client_android.util.AvatarLoader;
import com.ylaneo.chat_client_android.manager.UserManager;
import com.ylaneo.chat_client_android.network.PushListener;
import com.ylaneo.chat_client_android.network.PushManager;
import com.ylaneo.chat_client_android.session.UserSession;
import com.ylaneo.chat_client_android.ui.LoginActivity;
import com.ylaneo.chat_client_android.ui.fragment.FriendsFragment;
import com.ylaneo.chat_client_android.ui.fragment.MessagesFragment;
import com.ylaneo.chat_client_android.ui.fragment.SearchFragment;

/**
 * 主界面：底部导航切换三个 Fragment
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private Fragment currentFragment;

    private final MessagesFragment messagesFragment = new MessagesFragment();
    private final FriendsFragment friendsFragment = new FriendsFragment();
    private final SearchFragment searchFragment = new SearchFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 检查登录状态
        if (!UserSession.getInstance().isLoggedIn()) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();
            if (id == R.id.nav_messages) {
                fragment = messagesFragment;
            } else if (id == R.id.nav_friends) {
                fragment = friendsFragment;
            } else if (id == R.id.nav_search) {
                fragment = searchFragment;
            } else {
                return false;
            }
            switchFragment(fragment);
            return true;
        });

        // 默认显示消息列表
        switchFragment(messagesFragment);
        bottomNav.setSelectedItemId(R.id.nav_messages);
    }

    private void switchFragment(Fragment fragment) {
        if (fragment == currentFragment) return;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (currentFragment != null) {
            ft.hide(currentFragment);
        }
        if (!fragment.isAdded()) {
            ft.add(R.id.fragment_container, fragment);
        } else {
            ft.show(fragment);
        }
        ft.commit();
        currentFragment = fragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, "退出登录");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == Menu.FIRST) {
            handleLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleLogout() {
        new AlertDialog.Builder(this)
                .setTitle("确认退出")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("退出", (dialog, which) -> {
                    Integer userId = UserSession.getInstance().getCurrentUserId();
                    if (userId != null) {
                        new Thread(() -> {
                            try {
                                UserManager.getInstance().updateUserStatus(userId, 0);
                            } catch (Exception ignore) {
                            }
                        }).start();
                    }

                    PushManager.getInstance().disconnect();
                    AvatarLoader.clearCache();
                    UserSession.getInstance().logout();
                    goToLogin();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
