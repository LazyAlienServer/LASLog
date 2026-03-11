export default function () {
  const chars = '0123456789abcdefghijklmnopqrstuvwxyz'
  const randomBytes = crypto.getRandomValues(new Uint8Array(32))
  return Array.from(randomBytes, byte => chars[byte % chars.length]).join('')
}
