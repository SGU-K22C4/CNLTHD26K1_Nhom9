import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../../../shared/components/ui/Button';
import Input from '../../../shared/components/ui/Input';
import { formatCurrency } from '../../../shared/utils/format';

const FREE_SHIPPING_THRESHOLD = 100;

/**
 * CartSummary — order breakdown + promo code + checkout button.
 *
 * Props:
 *   subtotal – number (sum of all items)
 */
export default function CartSummary({ subtotal = 0 }) {
  const navigate = useNavigate();
  const [promoCode, setPromoCode] = useState('');
  const [promoApplied, setPromoApplied] = useState(false);
  const [promoError, setPromoError] = useState('');

  const shippingFee = subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : 9.99;
  const discount = promoApplied ? subtotal * 0.1 : 0; // 10% mock discount
  const total = subtotal - discount + shippingFee;

  const handleApplyPromo = () => {
    if (promoCode.trim().toUpperCase() === 'MODIMAL10') {
      setPromoApplied(true);
      setPromoError('');
    } else {
      setPromoApplied(false);
      setPromoError('Invalid promo code.');
    }
  };

  return (
    <div className="bg-white border border-neutral-cbcbcb p-6 flex flex-col gap-5">
      {/* Heading */}
      <h2 className="font-[Montserrat] font-bold text-[16px] leading-[1.4] text-ink capitalize">
        Order Summary
      </h2>

      {/* Line items */}
      <div className="flex flex-col gap-3">
        <SummaryRow label="Subtotal" value={formatCurrency(subtotal)} />

        <SummaryRow
          label="Shipping"
          value={shippingFee === 0 ? 'Miễn phí' : formatCurrency(shippingFee)}
          valueClass={shippingFee === 0 ? 'text-brand-green font-semibold' : undefined}
        />

        {shippingFee > 0 && (
          <p className="font-[Montserrat] text-[12px] text-neutral-404040 leading-[1.6] tracking-[0.8px] uppercase">
            Mua thêm {formatCurrency(FREE_SHIPPING_THRESHOLD - subtotal)} để được miễn phí vận chuyển
          </p>
        )}

        {promoApplied && (
          <SummaryRow
            label="Discount (10%)"
            value={`-${formatCurrency(discount)}`}
            valueClass="text-brand-green"
          />
        )}
      </div>

      {/* Divider */}
      <hr className="border-neutral-cbcbcb" />

      {/* Promo code */}
      <div className="flex flex-col gap-2">
        <p className="font-[Montserrat] font-semibold text-[12px] text-neutral-404040 uppercase tracking-[0.8px]">
          Promo Code
        </p>
        <div className="flex gap-2">
          <Input
            placeholder="Enter code"
            value={promoCode}
            onChange={(e) => {
              setPromoCode(e.target.value);
              setPromoError('');
            }}
            error={promoError}
            className="!rounded-none text-[14px] font-[Montserrat]"
          />
          <button
            onClick={handleApplyPromo}
            disabled={!promoCode.trim()}
            className="flex-shrink-0 px-4 h-[42px] bg-brand-olive text-white font-[Montserrat] text-[14px] hover:bg-brand-green transition-colors disabled:opacity-50 disabled:cursor-not-allowed capitalize"
          >
            Apply
          </button>
        </div>
        {promoApplied && (
          <p className="font-[Montserrat] text-[12px] text-brand-green">
            Promo code applied!
          </p>
        )}
      </div>

      {/* Divider */}
      <hr className="border-neutral-cbcbcb" />

      {/* Total */}
      <div className="flex items-center justify-between">
        <span className="font-[Montserrat] font-bold text-[16px] text-ink capitalize">Total</span>
        <span className="font-[Montserrat] font-bold text-[18px] text-ink">{formatCurrency(total)}</span>
      </div>

      {/* Checkout button */}
      <Button
        variant="primary"
        fullWidth
        onClick={() => navigate('/checkout')}
        className="!bg-brand-olive hover:!bg-brand-green !rounded-none !py-3 !font-[Montserrat] !font-normal !text-[14px] !tracking-wide capitalize"
      >
        Proceed to Checkout
      </Button>

      {/* Secure note */}
      <p className="font-[Montserrat] text-[12px] text-neutral-cbcbcb text-center leading-[1.6]">
        Secure checkout · Free returns
      </p>
    </div>
  );
}

function SummaryRow({ label, value, valueClass = '' }) {
  return (
    <div className="flex items-center justify-between">
      <span className="font-[Montserrat] text-[14px] text-neutral-404040 capitalize">{label}</span>
      <span className={`font-[Montserrat] text-[14px] text-ink ${valueClass}`}>{value}</span>
    </div>
  );
}
