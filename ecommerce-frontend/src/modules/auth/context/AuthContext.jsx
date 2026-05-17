import { createContext, useState, useCallback, useEffect } from 'react';
import { authService } from '../services/authService';

const AuthContext = createContext(null);
const AUTH_CLEARED_EVENT = 'auth:cleared';

function getStoredUser() {
    try {
        const userInfo = localStorage.getItem('userInfo');
        return userInfo ? JSON.parse(userInfo) : null;
    } catch {
        localStorage.removeItem('userInfo');
        return null;
    }
}

function normalizeAuthPayload(rawData) {
    const payload = rawData?.data && typeof rawData.data === 'object' ? rawData.data : rawData;

    const accessToken = payload?.accessToken || payload?.token || null;
    const refreshToken = payload?.refreshToken || null;

    return {
        accessToken,
        refreshToken,
        email: payload?.email || payload?.user?.email || null,
        firstName: payload?.firstName || payload?.user?.firstName || '',
        lastName: payload?.lastName || payload?.user?.lastName || '',
        role: payload?.role || payload?.user?.role || null,
        id: payload?.id || payload?.userId || payload?.user?.id || null,
    };
}

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(() => getStoredUser());

    const login = useCallback((rawData, navigateTo) => {
        const data = normalizeAuthPayload(rawData);
        if (!data.accessToken) {
            throw new Error('Login response missing access token');
        }

        const userInfo = {
            email: data.email,
            firstName: data.firstName,
            lastName: data.lastName,
            role: data.role,
            id: data.id,
        };
        setUser(userInfo);
        localStorage.setItem('accessToken', data.accessToken);
        if (data.refreshToken) {
            localStorage.setItem('refreshToken', data.refreshToken);
        } else {
            localStorage.removeItem('refreshToken');
        }
        localStorage.setItem('userInfo', JSON.stringify(userInfo));
        // Cho phép caller tự redirect (ví dụ: về trang trước khi bị đá ra)
        if (navigateTo) navigateTo();
    }, []);

    const logout = useCallback(async (navigate) => {
        // 1. Cố gắng báo backend revoke Refresh Token (best-effort, không chặn logout nếu lỗi)
        try {
            await authService.logout();
        } catch (err) {
            console.warn('[Auth] Logout API failed, continuing local cleanup:', err);
        }
        // 2. Xóa toàn bộ dữ liệu phiên đăng nhập
        setUser(null);
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userInfo');
        // 3. Chuyển hướng về trang chủ / login
        if (navigate) navigate('/');
    }, []);

    return (
        <AuthContext.Provider value={{ user, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export default AuthContext;
