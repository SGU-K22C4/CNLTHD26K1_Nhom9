import { useState, useEffect } from 'react';
import { MapPin, Plus } from 'lucide-react';
import AddressModal from './AddressModal';
import { userService } from '../services/userService';

export default function AddressManager() {
  const [addresses, setAddresses] = useState([]);
  const [isAddressModalOpen, setIsAddressModalOpen] = useState(false);
  const [selectedAddress, setSelectedAddress] = useState(null);

  const loadAddresses = async () => {
    try {
      const data = await userService.getAddresses();
      setAddresses(data || []);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    let mounted = true;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (mounted) loadAddresses();
    return () => { mounted = false; };
  }, []);

  const handleSaveAddress = async (addressData, id) => {
    if (id) {
      await userService.updateAddress(id, addressData);
    } else {
      await userService.addAddress(addressData);
    }
    await loadAddresses();
  };

  const handleDeleteAddress = async (id) => {
    if (window.confirm('Bạn có chắc muốn xóa địa chỉ này?')) {
      try {
        await userService.deleteAddress(id);
        await loadAddresses();
      } catch {
        alert('Tạm thời không thể xóa địa chỉ lúc này.');
      }
    }
  };

  return (
    <div className="bg-white border border-[#E8E8E8]">
      <div className="px-6 py-4 border-b border-[#E8E8E8] flex justify-between items-center">
        <div className="flex items-center gap-2">
          <MapPin size={16} className="text-[#202020]" />
          <h3 className="text-[15px] font-semibold text-[#202020]">Sổ địa chỉ</h3>
        </div>
        <button 
          onClick={() => {
            setSelectedAddress(null);
            setIsAddressModalOpen(true);
          }}
          className="text-[12px] text-[#5A6D57] hover:text-[#748C70] flex items-center gap-1 font-medium transition-colors"
        >
          <Plus size={14} /> Thêm địa chỉ mới
        </button>
      </div>

      <div className="p-6">
        {addresses.length === 0 ? (
          <div className="text-center py-6 text-gray-500 text-[13px]">
            Bạn chưa có địa chỉ giao hàng nào.
          </div>
        ) : (
          <div className="space-y-4">
            {addresses.map((addr) => (
              <div key={addr.id} className="border border-[#EFEFEF] rounded p-4 flex justify-between items-start">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-[14px] font-semibold text-[#202020]">{addr.fullName}</span>
                    {addr.isDefault && (
                      <span className="text-[10px] bg-[#5A6D57] text-white px-2 py-0.5 tracking-wide rounded-sm uppercase">
                        Mặc định
                      </span>
                    )}
                  </div>
                  <p className="text-[13px] text-[#666] mb-1">SĐT: {addr.phoneNumber}</p>
                  <p className="text-[13px] text-[#666] leading-relaxed">
                    {addr.street}<br/>
                    {addr.ward}, {addr.city}
                  </p>
                </div>
                <div className="flex flex-col gap-2 items-end">
                  <button 
                    onClick={() => {
                      setSelectedAddress(addr);
                      setIsAddressModalOpen(true);
                    }}
                    className="text-[12px] font-medium text-[#5A6D57] hover:underline"
                  >
                    Chỉnh sửa
                  </button>
                  {!addr.isDefault && (
                    <button 
                      onClick={() => handleDeleteAddress(addr.id)}
                      className="text-[12px] font-medium text-red-500 hover:underline flex items-center gap-1"
                    >
                      Xóa
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <AddressModal
        isOpen={isAddressModalOpen}
        onClose={() => setIsAddressModalOpen(false)}
        initialData={selectedAddress}
        onSave={handleSaveAddress}
      />
    </div>
  );
}
