import { createContext, useState, useEffect, useCallback } from 'react';
import { authService } from '../services/authService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);

    useEffect(() => {
        const userInfo = localStorage.getItem('userInfo');
        if (userInfo) {
            setUser(JSON.parse(userInfo));
        }
    }, []);

    const login = useCallback((data, navigateTo) => {
        const userInfo = {
            email: data.email,
            firstName: data.firstName,
            lastName: data.lastName,
            role: data.role
        };
        setUser(userInfo);
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);
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