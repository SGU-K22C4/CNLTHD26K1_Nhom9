import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import Modal from '@/shared/components/ui/Modal';
import Input from '@/shared/components/ui/Input';
import AddressFields from '@/shared/components/ui/AddressFields';
import { useProvinces } from '@/shared/hooks/useProvinces';

export default function AddressModal({ isOpen, onClose, onSave, initialData }) {
  const { provinces, loading } = useProvinces(2);
  const [wards, setWards] = useState([]);

  const isEdit = !!initialData;

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      fullName: '',
      phoneNumber: '',
      street: '',
      cityCode: '',
      wardCode: '',
      isDefault: false
    }
  });

  // Watch for changes on the selected city
  const selectedCityCode = watch('cityCode');

  // When selectedCitycode changes, update the wards dropdown
  useEffect(() => {
    if (selectedCityCode && !loading) {
      const city = provinces.find(p => p.code == selectedCityCode);
      setWards(city && city.wards ? city.wards : []);
      // Only clear wardCode if we are not initializing from initialData where ward already matches
      // But it's safer to just let the user re-select if they change city.
    } else {
      setWards([]);
    }
  }, [selectedCityCode, provinces, loading]);

  // When initialData changes (Modal open for edit)
  useEffect(() => {
    if (isOpen) {
      if (initialData) {
        // Find city code by name
        let matchedCityCode = '';
        let matchedWardCode = '';
        
        if (!loading && provinces.length > 0) {
          const city = provinces.find(p => p.name === initialData.city);
          if (city) {
            matchedCityCode = city.code;
            const cityWards = city.wards || [];
            const ward = cityWards.find(w => w.name === initialData.ward);
            if (ward) {
              matchedWardCode = ward.code;
            }
          }
        }

        reset({
          fullName: initialData.fullName || '',
          phoneNumber: initialData.phoneNumber || '',
          street: initialData.street || '',
          cityCode: matchedCityCode || '',
          wardCode: matchedWardCode || '',
          isDefault: initialData.isDefault || false
        });
      } else {
        reset({
          fullName: '',
          phoneNumber: '',
          street: '',
          cityCode: '',
          wardCode: '',
          isDefault: false
        });
      }
    }
  }, [initialData, isOpen, reset, provinces, loading]);

  const onSubmit = async (data) => {
    try {
      // Get names from codes
      const city = provinces.find(p => p.code == data.cityCode);
      const ward = wards.find(w => w.code == data.wardCode);

      const payload = {
        fullName: data.fullName,
        phoneNumber: data.phoneNumber,
        street: data.street,
        city: city ? city.name : '',
        ward: ward ? ward.name : '',
        isDefault: data.isDefault
      };

      await onSave(payload, isEdit ? initialData.id : null);
      onClose(); // only close on success
    } catch (err) {
      console.error('Failed to save address:', err);
      alert('Đã xảy ra lỗi khi lưu địa chỉ: ' + (err.response?.data?.message || err.message));
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEdit ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ mới'}>
      <div className="p-6">
        <h2 className="text-xl font-bold mb-4">{isEdit ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ mới'}</h2>
        
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <Input
            type="text"
            placeholder="Họ và tên người nhận"
            error={errors.fullName?.message}
            {...register('fullName', { required: 'Vui lòng nhập họ tên' })}
          />

          <Input
            type="text"
            placeholder="Số điện thoại"
            error={errors.phoneNumber?.message}
            {...register('phoneNumber', { 
              required: 'Vui lòng nhập số điện thoại',
              pattern: {
                value: /^(84|0[3|5|7|8|9])+([0-9]{8})$/,
                message: 'Số điện thoại không hợp lệ',
              }
            })}
          />

          <AddressFields
            streetInputProps={register('street', { required: 'Vui lòng nhập địa chỉ cụ thể' })}
            streetError={errors.street?.message}
            citySelectProps={register('cityCode', { required: 'Vui lòng chọn Tỉnh/Thành' })}
            cityError={errors.cityCode?.message}
            wardSelectProps={{
              ...register('wardCode', { required: 'Vui lòng chọn Quận/Huyện/Phường' }),
              disabled: !wards.length,
            }}
            wardError={errors.wardCode?.message}
            provinces={provinces}
            wards={wards}
            selectClassName="w-full px-4 border h-[46px] bg-[#F9F9F9] rounded focus:bg-white focus:border-[#5A6D57] transition-colors text-sm text-gray-700"
            selectErrorClassName="border-red-500"
            selectDefaultClassName="border-[#E5E7EB]"
          />

          <label className="flex items-center gap-2 cursor-pointer mt-2">
            <input type="checkbox" {...register('isDefault')} className="w-4 h-4 accent-[#5A6D57]" />
            <span className="text-sm text-gray-700">Đặt làm địa chỉ mặc định</span>
          </label>

          <div className="flex justify-end gap-3 mt-4">
            <button
              type="button"
              onClick={onClose}
              className="px-6 py-2 border border-gray-300 rounded text-sm font-medium hover:bg-gray-50 transition-colors"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-6 py-2 bg-[#5A6D57] text-white rounded text-sm font-medium hover:bg-[#4a5c48] transition-colors disabled:opacity-50"
            >
              {isSubmitting ? 'Đang lưu...' : 'Lưu địa chỉ'}
            </button>
          </div>
        </form>
      </div>
    </Modal>
  );
}
