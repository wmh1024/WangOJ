export enum AUTH {
  ADMIN = 'admin',
  USER = 'user'
}

/** 判断用户角色是否为管理员。 */
export const isAdmin = (role?: string | null): boolean => role === AUTH.ADMIN

/**
 * 首页轮播图。
 *
 * 使用仓库内的本地示例图，避免依赖个人对象存储或第三方 CDN。
 * 部署后可替换为 /public/banners/ 下的自有图片。
 */
export const HOME_BANNERS: string[] = [
  '/banners/banner-1.svg',
  '/banners/banner-2.svg',
  '/banners/banner-3.svg'
]
