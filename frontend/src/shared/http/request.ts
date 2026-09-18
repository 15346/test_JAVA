// ============================================================
//  统一请求层：所有业务 API 都从这里发请求。
//  - 固定 same-origin 携带 Cookie；
//  - 自动合并 JSON 头；
//  - 非安全方法自动带上 CSRF 头（先确保 /api/auth/csrf 已请求过）；
//  - 统一把后端 { code, message } 错误包成 ApiError。
//  注意：绝不把密码、令牌或 Cookie 写入日志。
// ============================================================

/** 后端统一错误体的封装 */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  override name = 'ApiError'

  constructor(status: number, code: string, message: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'
const CSRF_ENDPOINT = '/api/auth/csrf'

/** 需要 CSRF 保护的“非安全”方法 */
const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

/** 从 document.cookie 中读取指定 Cookie（找不到返回 null） */
function readCookie(name: string): string | null {
  const pattern = new RegExp(`(?:^|;\\s*)${name}=([^;]*)`)
  const matched = document.cookie.match(pattern)
  return matched ? decodeURIComponent(matched[1]) : null
}

/** 首次使用时的 CSRF 拉取，"只发一次"通过 Promise 记忆化实现 */
let csrfPrimed: Promise<void> | null = null

/** 确保 CSRF Cookie 已就绪（已有 Cookie 则直接返回） */
function ensureCsrfPrimed(): Promise<void> {
  if (readCookie(CSRF_COOKIE)) {
    return Promise.resolve()
  }
  if (!csrfPrimed) {
    csrfPrimed = fetch(CSRF_ENDPOINT, {
      method: 'GET',
      credentials: 'same-origin',
    })
      .then(() => undefined)
      .catch(() => {
        // 拉取失败则清空记忆，后续非安全请求可以重试
        csrfPrimed = null
        return undefined
      })
  }
  return csrfPrimed
}

/** 统一发请求：返回解析后的 JSON，204 返回 undefined，非 2xx 抛 ApiError */
export async function request<T>(url: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()

  const csrfHeaders: Record<string, string> = {}

  if (UNSAFE_METHODS.has(method)) {
    await ensureCsrfPrimed()
    const token = readCookie(CSRF_COOKIE)
    if (token) {
      csrfHeaders[CSRF_HEADER] = token
    }
  }

  const response = await fetch(url, {
    ...init,
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(init.headers as Record<string, string> | undefined),
      ...csrfHeaders,
    },
    credentials: 'same-origin',
  })

  if (response.status === 204) {
    return undefined as unknown as T
  }

  if (!response.ok) {
    let code = 'UNKNOWN'
    let message = `请求失败（${response.status}）`
    try {
      const body = (await response.json()) as { code?: unknown; message?: unknown }
      if (body && typeof body === 'object') {
        if (typeof body.code === 'string') code = body.code
        if (typeof body.message === 'string') message = body.message
      }
    } catch {
      // 解析失败则沿用通用错误信息
    }
    throw new ApiError(response.status, code, message)
  }

  return (await response.json()) as T
}
