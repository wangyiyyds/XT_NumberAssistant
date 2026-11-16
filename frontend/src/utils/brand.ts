/**
 * 显示层品牌兜底：后端（预置数据 / 历史会话）仍可能返回「珠峰」文案，
 * 前端统一替换为「先霆」，保证界面只显示新品牌名。
 * 只匹配中文「珠峰」，不影响 brand: 'zhufeng'、test@zhufeng.cn 这类接口字段与账号。
 */
const BRAND_FORMER = '珠峰'
const BRAND_CURRENT = '先霆'

/** 替换单个字符串中的旧品牌名。 */
export function brandText(text: string): string {
  return text.split(BRAND_FORMER).join(BRAND_CURRENT)
}

/**
 * 递归替换接口返回数据里所有字符串字段的旧品牌名。
 * 只处理纯 JSON 数据，不考虑循环引用；对 null / 原始类型直接返回。
 */
export function brandData<T>(value: T): T {
  if (typeof value === 'string') {
    return brandText(value) as unknown as T
  }

  if (Array.isArray(value)) {
    return value.map((item) => brandData(item)) as unknown as T
  }

  if (value && typeof value === 'object') {
    const source = value as Record<string, unknown>
    const target: Record<string, unknown> = {}
    for (const key of Object.keys(source)) {
      target[key] = brandData(source[key])
    }
    return target as unknown as T
  }

  return value
}
