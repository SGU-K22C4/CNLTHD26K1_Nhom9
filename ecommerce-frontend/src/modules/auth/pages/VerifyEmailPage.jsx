import { useEffect, useState, useRef } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import axios from 'axios';
import { API_CONFIG } from '../../../config/api.config';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [status, setStatus] = useState('loading'); // loading, success, error

  // Dùng useRef để chặn useEffect gọi API 2 lần trong Strict Mode
  const hasFetched = useRef(false);

  useEffect(() => {
    if (!token) {
      setStatus('error');
      return;
    }

    if (hasFetched.current) return;
    hasFetched.current = true;

    const verifyToken = async () => {
      try {
        await axios.get(`${API_CONFIG.BASE_URL}/api/v1/auth/verify-email?token=${token}`);
        setStatus('success');
      } catch (err) {
        console.error("Lỗi xác thực:", err);
        setStatus('error');
      }
    };

    verifyToken();
  }, [token]);

  return (
    <div className="flex flex-col items-center justify-center min-h-[calc(100vh-120px)] bg-gray-50 px-4 py-12">
      <div className="max-w-md w-full bg-white p-8 rounded shadow-sm text-center">
        {status === 'loading' && (
          <>
            <h2 className="text-2xl font-semibold mb-4">Đang xác thực...</h2>
            <p className="text-gray-600">Xin vui lòng chờ một lát trong khi chúng tôi kiểm tra thông tin.</p>
          </>
        )}

        {status === 'success' && (
          <>
            <svg className="w-16 h-16 text-green-500 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
            </svg>
            <h2 className="text-2xl font-semibold mb-2 text-gray-900">Xác Thực Thành Công!</h2>
            <p className="text-gray-600 mb-6">Tài khoản của bạn đã được kích hoạt. Hãy đăng nhập để bắt đầu mua sắm.</p>
            <Link to="/login" className="px-6 py-3 bg-[#5A6D57] text-white rounded font-medium hover:bg-[#4a5c48] transition-colors">
              Tới Trang Đăng Nhập
            </Link>
          </>
        )}

        {status === 'error' && (
          <>
            <svg className="w-16 h-16 text-red-500 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
            <h2 className="text-2xl font-semibold mb-2 text-gray-900">Xác Thực Thất Bại</h2>
            <p className="text-gray-600 mb-6">Link xác nhận không hợp lệ hoặc đã hết hạn.</p>
            <Link to="/login" className="px-6 py-3 bg-gray-900 text-white rounded font-medium hover:bg-black transition-colors">
              Quay về Trang chủ
            </Link>
          </>
        )}
      </div>
    </div>
  );
}