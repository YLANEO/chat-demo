<script setup lang="ts">
import { RouterView, useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, SwitchButton, ChatDotRound } from '@element-plus/icons-vue'

const userInfoStore = useUserStore()
// 创建一个实例
const router = useRouter();
const route = useRoute()   // ← 加这一行
// 接受需要执行操作的函数
const handleCommand = (command) => {
  // 判断是不是退出登录，是的话执行，不然进行路由操作
  if (command == "logout") {
    // 这里直接使用与articlecategory中相同的模块，弹窗
    ElMessageBox.confirm("你确认要退出登录吗?", "温馨提示", {
      confirmButtonText: "确认",
      cancelButtonText: "取消",
      type: "warning",
    })
      // 执行异步函数请求
      .then(async () => {
        //调用接口
        // 退出登录：删除pinia中的token和用户持久化信息；
        userInfoStore.clearUser()
        // 跳转到登录页面
        router.push("/login");

        ElMessage({ type: "success", message: "退出登录成功", });
      })
      // 进行异常捕获
      .catch(() => {
        ElMessage({ type: "info", message: "用户取消了退出登录", });
      });
  }
  // 不是退出登录操作
  else {
    // 路由操作
    router.push("/main/" + command);
  }
};

// function handleLogout() {
//   userInfoStore.clearUser()
//   //跳转登录页
// }

</script>
<template><!-- 全屏背景层 -->
<div class="page-background"></div>
<div class="layout-wrapper">
  <!-- 左侧导航栏 -->
  <aside class="glass-sidebar">
    <!-- 导航菜单 -->
    <el-menu active-text-color="var(--theme-accent)" text-color="var(--theme-text)" :default-active="route.path" class="glass-menu" router>
      <!-- 聊天页面 -->
      <el-menu-item index="/main/chat">
        <el-icon>
          <ChatDotRound />
        </el-icon>
        <span>聊天</span>
      </el-menu-item>
      <!--  -->
      <el-menu-item index="/main/friends">
        <el-icon>
          <User />
        </el-icon>
        <span>好友</span>
      </el-menu-item>
      <!--  -->
      <!-- <el-menu-item index="/main/profile">
        <el-icon>
          <Setting />
        </el-icon>
        <span>个人信息 </span>
      </el-menu-item> -->
      <!--  -->
      <!-- <el-menu-item index="/main/home">
        <el-icon>
          <House />
        </el-icon>
        <span>首页</span>
      </el-menu-item> -->
      <!-- 退出登录：独立的按钮，不要混在 el-menu-item 里 -->
      <el-menu-item class="logout-item" @click="handleCommand('logout')">
        <el-icon>
          <SwitchButton />
        </el-icon>
        <span>退出登录</span>
      </el-menu-item>
    </el-menu>
  </aside>
  <!-- 右侧内容区 -->
  <div class="main-area">
    <!-- 顶栏 -->
    <header class="glass-header">
      <!-- 角色状态信息 -->
      <div class="header-status">
        <div class="status-chip role-chip">
          <!-- <span class="chip-dot"></span> -->
          <span class="chip-label">用户</span>
          <span class="chip-value">{{ userInfoStore.nickname ? userInfoStore.nickname : userInfoStore.username }}</span>
        </div>
      </div>
      <!-- 用户下拉菜单 -->
      <!-- <el-dropdown placement="bottom-end" @command="handleCommand"> -->
      <el-dropdown placement="bottom-end" @command="handleCommand">
        <div class="avatar-wrapper">
          <!-- <el-avatar :src="userInfoStore.avatarUrl ? userInfoStore.avatarUrl : avatar" class="avatar" /> -->
          <el-avatar :src="userInfoStore.avatarUrl" class="avatar" />
          <div class="avatar-glow"></div>
        </div>
        <template #dropdown>
          <el-dropdown-menu class="glass-dropdown">
            <!-- <el-dropdown-item command="info" :icon="User">个人信息</el-dropdown-item>
            <el-dropdown-item command="avatar" :icon="Crop">更换头像</el-dropdown-item>
            <el-dropdown-item command="resetPassword" :icon="EditPen">重置密码</el-dropdown-item> -->
            <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>
    <!-- 内容区域 -->
    <main class="main-content">
      <!-- 内容内容区域 -->
      <div class="content-card">
        <RouterView />
      </div>
    </main>
  </div>
</div>
</template>
<!-- <style lang="scss" scoped> -->
<style lang="scss" scoped>
/* ============================
  全屏渐变背景
   ============================ */
.page-background {
  position: fixed;
  inset: 0;
  min-height: 100vh;
  background: var(--theme-body-bg);
  z-index: -3;
  transition: background 0.6s ease;
}

/* ============================
  整体布局
   ============================ */
.layout-wrapper {
  display: flex;
  height: 100vh;
  overflow: hidden; //新增：确保永远不产生自己的滚动条
  position: relative;
  z-index: 0;
}

/* ============================
  毛玻璃侧边栏
   ============================ */
.glass-sidebar {
  width: 130px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  height: 100vh;
  align-self: flex-start;
  display: flex;
  flex-direction: column;
  background: var(--theme-sidebar-bg);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-right: 1px solid var(--theme-border);
  transition: background 0.6s ease, border-color 0.6s ease;
  overflow: hidden;
}


/* ============================
  菜单（穿透 Element Plus）
   ============================ */
.glass-menu {
  flex: 1;
  overflow-y: auto;
  border-right: none;
  background: transparent;
  padding: 10px 0;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--theme-border);
    border-radius: 2px;
  }
}

// 退出登录按钮
.logout-item {
  padding-top: auto;
}

/* 穿透 el-menu 内部 —— Element Plus 菜单项 */
:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  margin: 2px 10px;
  border-radius: 10px;
  padding-left: 14px !important;
  height: 44px;
  line-height: 44px;
  transition: all 0.25s ease;
  color: var(--theme-text);

  &:hover {
    background-color: var(--theme-menu-hover) !important;
  }

  &.is-active {
    background-color: var(--theme-menu-active) !important;
    color: var(--theme-accent) !important;
  }

  .el-icon {
    color: inherit;
    margin-right: 10px;
    font-size: 16px;
  }

  span {
    font-size: 14px;
    font-weight: 500;
    letter-spacing: 0.3px;
  }
}

:deep(.el-menu--inline) {
  background: transparent !important;
  border: none;
  padding-left: 6px;

  .el-menu-item {
    padding-left: 44px !important;
    font-size: 13px;
    height: 40px;
    line-height: 40px;
  }
}

:deep(.el-sub-menu__icon-arrow) {
  color: var(--theme-text-muted);
}

/* ============================
  右侧主区域
   ============================ */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0; //防止出现滚动条
}

/* ============================
  顶栏
   ============================ */
.glass-header {
  height: 64px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: var(--theme-header-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--theme-border);
  transition: background 0.6s ease, border-color 0.6s ease;
  gap: 16px;
}

.header-status {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  flex: 1;
}

/* 状态标签芯片 */
.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 20px;
  background: var(--theme-card-bg);
  border: 1px solid var(--theme-border);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  transition: background 0.6s ease;
}

.chip-dot {
  width: 7px;
  height: 7px;
  border-radius: 5px;
  background: var(--theme-accent);
  // box-shadow: 0 0 6px var(--theme-accent);
  flex-shrink: 0;
  // animation: pulse 2s ease-in-out infinite
}

.status-online .chip-dot {
  background: #2ecc71;
  box-shadow: 0 0 6px #2ecc71;
}

.status-offline .chip-dot {
  background: #e74c3c;
  box-shadow: 0 0 6px #e74c3c;
  animation: none;
}

.chip-label {
  font-size: 12px;
  color: var(--theme-text-muted);
  font-weight: 400;
}

.chip-value {
  font-size: 13px;
  color: var(--theme-text);
  font-weight: 600;
}

/* 按钮 */
.glass-btn {
  background: var(--theme-btn-bg) !important;
  border: 1px solid var(--theme-border) !important;
  color: var(--theme-text) !important;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 20px;
  font-size: 12px;
  padding: 5px 14px;
  height: auto;
  transition: background 0.25s ease, transform 0.15s ease;

  &:hover {
    background: var(--theme-btn-hover) !important;
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }
}

/* 头像区域 */
.avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 5px;
  padding: 1px;
  border: 0px solid var(--theme-border);
  // transition: border-color 0.3s ease;

  // &:hover {
  //   border-color: var(--theme-accent);

  //   .avatar-glow {
  //     opacity: 1;
  //     transform: scale(1.2);
  //   }
  // }
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 5px;
  display: block;
  object-fit: cover;
}


/* 下拉菜单（穿透 Element Plus） */
.glass-dropdown {
  background: var(--theme-dropdown-bg) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 0px solid var(--theme-border) !important;
  border-radius: 5px !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3) !important;
  transition: background 0.6s ease;
}

:deep(.el-dropdown-menu__item) {
  color: var(--theme-text) !important;
  font-size: 13.5px;
  border-radius: 8px;
  margin: 3px 8px;
  padding: 8px 12px;

  &:hover {
    background: var(--theme-menu-hover) !important;
    color: var(--theme-accent) !important;
  }

  .el-icon {
    color: var(--theme-text-muted);
  }
}

:deep(.el-dropdown-menu__item--divided) {
  border-top: 1px solid var(--theme-border);
  margin-top: 6px;
}

/* ============================
  主内容区
   ============================ */
.main-content {
  flex: 1;
  min-height: 0; //防止出现滚动条
  height: 100%;
  padding: 5px;
  display: flex; // ← 新增：让 content-card 能撑满
  flex-direction: column; // ← 新增

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--theme-border);
    border-radius: 3px;
  }
}

.content-card {
  min-height: 0; //防止出现滚动条
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  background: var(--theme-card-bg);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--theme-border);
  border-radius: 2px;
  padding: 0px;
  transition: background 0.6s ease, border-color 0.6s ease;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}
</style>
