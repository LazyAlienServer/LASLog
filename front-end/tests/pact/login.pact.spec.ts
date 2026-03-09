import { MatchersV3, PactV3 } from '@pact-foundation/pact'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

/**
 * 1. 定义严格的响应接口
 * 这不仅是为了测试，也是你前端业务代码中应该使用的类型
 */
interface LoginResponse {
  code: number
  message: string
  data: {
    accessToken: string
    refreshToken: string
    expiresIn: number
    time: number
    tokenType: 'Bearer'
    user: {
      id: number
      username: string
      roles: string[]
    }
  }
}

// 初始化 Pact 角色
const provider = new PactV3({
  consumer: 'NuxtFrontEnd',
  provider: 'SpringBootBackEnd',
  dir: path.resolve(process.cwd(), 'pacts'),
})

describe('login API Contract', () => {
  it('should return a valid token set when credentials are correct', async () => {
    // 2. 定义契约交互（预期后端长什么样）
    provider.addInteraction({
      states: [{ description: 'user admin exists' }],
      uponReceiving: 'a request for login with valid credentials',
      withRequest: {
        method: 'POST',
        path: '/api/auth/login',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
        body: {
          username: 'admin',
          password: 'password_123', // 保持测试用例一致
        },
      },
      willRespondWith: {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
        body: {
          code: 200,
          message: 'success',
          data: {
            // Matchers
            accessToken: MatchersV3.string('eyJhbGciOiJIUzI1Ni'),
            refreshToken: MatchersV3.string('eyJhbGciOiJIUzI1Ni5836UhUSGDGGGGDG'), // 验证必须是 UUID 格式
            expiresIn: MatchersV3.integer(3600),
            time: MatchersV3.integer(1738224000),
            tokenType: MatchersV3.equal('Bearer'),
            user: {
              id: MatchersV3.integer(1),
              username: MatchersV3.string('admin'),
              roles: MatchersV3.eachLike('ROLE_ADMIN'), // 验证是字符串数组
            },
          },
        },
      },
    })

    // 3. 执行测试
    await provider.executeTest(async (mockServer) => {
      const response = await fetch(`${mockServer.url}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
        body: JSON.stringify({
          username: 'admin',
          password: 'password_123',
        }),
      })

      const result = (await response.json()) as LoginResponse

      // 验证状态码
      expect(response.status).toBe(200)

      // 验证业务字段
      expect(result.code).toBe(200)
      expect(result.data.tokenType).toBe('Bearer')
      expect(Array.isArray(result.data.user.roles)).toBe(true)

      // 验证时间戳（确保是数字且在合理范围内）
      expect(result.data.time).toBeGreaterThan(1700000000)
    })
  })
})
