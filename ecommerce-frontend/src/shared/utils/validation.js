export const validateEmail = (email) => {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return re.test(email);
};

export const validatePhone = (phone) => {
  const re = /^(0|\+84)[0-9]{9}$/;
  return re.test(phone);
};

export const validatePassword = (password) => {
  // Ít nhất 8 ký tự, có chữ hoa, chữ thường, số
  const re = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,}$/;
  return re.test(password);
};

export const getPasswordStrength = (password) => {
  let strength = 0;
  if (password.length >= 8) strength++;
  if (password.length >= 12) strength++;
  if (/[a-z]/.test(password)) strength++;
  if (/[A-Z]/.test(password)) strength++;
  if (/[0-9]/.test(password)) strength++;
  if (/[^a-zA-Z0-9]/.test(password)) strength++;
  
  if (strength <= 2) return { label: 'Yếu', color: 'red' };
  if (strength <= 4) return { label: 'Trung bình', color: 'yellow' };
  return { label: 'Mạnh', color: 'green' };
};