import request from './request'
import type { LoginForm, RegisterForm, User } from '@/types/api'

// POST /api/user/login -> 返回User对象，data表示这个形参，pick表示从LoginForm类型中挑选若干个属性组成一个新的类型
export const loginService = (loginData: Pick<LoginForm, 'username' | 'password'>) => {
  const params = new URLSearchParams();
  //把const换成let也行，但是ts的类型检查会报错
  for (const key in loginData) {
    params.append(key, loginData[key]);
  }
  return request.post('/user/login', params);
}
// POST /api/user/register  -> 后端只返回成功
export const registerService = (data: RegisterForm): Promise<void> =>
  request.post('/user/register', data)

//获取用户信息,不用参数，因为进行请求时会自带token信息
export const getUserInfoService = (): Promise<User> => {
  return request.get('/user/info')
}
