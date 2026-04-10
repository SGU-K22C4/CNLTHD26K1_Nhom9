function getGuestUserId() {
  let id = localStorage.getItem('guestUserId')
  if (!id) {
    id = crypto.randomUUID()
    localStorage.setItem('guestUserId', id)
  }
  return id
}

function decodeJwtPayload(token) {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null

    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4)

    return JSON.parse(atob(padded))
  } catch {
    return null
  }
}

function getUserIdFromUserInfo() {
  try {
    const raw = localStorage.getItem('userInfo')
    if (!raw) return null

    const parsed = JSON.parse(raw)
    return parsed?.id || parsed?.userId || parsed?.email || null
  } catch {
    return null
  }
}

export function getCurrentUserId() {
  const token = localStorage.getItem('accessToken')
  if (token) {
    const payload = decodeJwtPayload(token)
    const userId = payload?.userId || payload?.sub || payload?.email
    if (userId) return userId
  }

  const userIdFromProfile = getUserIdFromUserInfo()
  if (userIdFromProfile) return userIdFromProfile

  return getGuestUserId()
}

export function buildUserHeaders() {
  return { 'X-User-Id': getCurrentUserId() }
}
