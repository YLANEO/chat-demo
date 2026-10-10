import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/userStore'

const routes: RouteRecordRaw[] = [
  // 公开路由
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue'), meta: { requiresAuth: false } },
  { path: '/register', name: 'Register', component: () => import('@/views/RegisterView.vue'), meta: { requiresAuth: false } },

  // 受保护的嵌套路由（带 Layout）
  {
    //默认路由
    path: '/main',
    component: () => import('@/views/MainLayoutView.vue'),
    meta: { requiresAuth: true },
    //子路由
    children: [
      { path: '', redirect: '/main/chat' },
      // { path: 'home', name: 'Home', component: () => import('@/views/HomeView.vue') },
      { path: 'friends', name: 'Friends', component: () => import('@/views/FriendsView.vue') },
      { path: 'chat', name: 'Chat', meta: { title: '聊天页面', icon: 'chat' }, component: () => import('@/views/ChatView.vue') },
      // { path: 'profile', name: 'Profile', component: () => import('@/views/ProfileView.vue') },
    ]
  },
]

// const router = createRouter({
//   history: createWebHistory(import.meta.env.BASE_URL),
//   routes,
// })

const router = createRouter({
  history: createWebHistory('/'),
  routes,
})

// router.beforeEach((to, from, next) => {
//   const userStore = useUserStore()
//   if (to.meta.requiresAuth && !userStore.isLoggedIn) {

//     next({ name: 'Login', query: { redirect: to.fullPath } })
//   } else {
//     next()
//   }
// })

//to表示即将访问的路由，from表示从哪个路由访问的。其实没有用到from，但是先写上。
// router.beforeEach((to, from) => {
router.beforeEach((to) => {

  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    //这里的return其实是原来废弃的next()的替代，所以直接return即可。
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
  // 不需要 return undefined，让路由正常通过即可
})

export default router
