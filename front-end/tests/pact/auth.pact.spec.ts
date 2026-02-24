import { PactV3 } from '@pact-foundation/pact'
import path from 'node:path'
import { describe, it } from 'vitest'

/**
 * 契约测试占位文件
 * 消费者: NuxtFrontEnd
 * 服务者: SpringBootBackEnd
 */

const provider = new PactV3({
  consumer: 'NuxtFrontEnd',
  provider: 'SpringBootBackEnd',
  dir: path.resolve(process.cwd(), 'pacts'),
})

describe('authentication API Contract', () => {
  // 示例占位测试：当没有任何交互定义时，Pact 会通过但不生成有效契约
  it('placeholder: should be implemented later', async () => {
    // TODO: 使用 provider.addInteraction() 定义具体的 API 预期

    await provider.executeTest(async (mockServer) => {
      // TODO: 在此处调用你的业务代码或 fetch 请求
      expect(mockServer.url).toContain('http://127.0.0.1')
      console.warn('Mock server is running at:', mockServer.url)
    })
  })
})
