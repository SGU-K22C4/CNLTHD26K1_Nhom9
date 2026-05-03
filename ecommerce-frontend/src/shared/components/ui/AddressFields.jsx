import Input from './Input';

export default function AddressFields({
  streetInputProps,
  streetError,
  citySelectProps,
  cityError,
  wardSelectProps,
  wardError,
  provinces,
  wards,
  cityPlaceholder = 'City / Province',
  wardPlaceholder = 'Ward',
  streetPlaceholder = 'Street Address (e.g. 460/4 Nơ Trang Long)',
  selectClassName = 'w-full px-4 border border-transparent h-[46px] bg-[#F9F9F9] rounded focus:bg-white focus:border-[#5A6D57] focus:outline-none transition-colors text-sm',
  selectErrorClassName = 'border-red-500',
  selectDefaultClassName = '',
  errorTextClassName = 'text-xs text-red-500 mt-1',
  wrapperClassName = 'grid grid-cols-1 sm:grid-cols-2 gap-3',
}) {
  const cityClassName = `${selectClassName} ${cityError ? selectErrorClassName : selectDefaultClassName}`.trim();
  const wardClassName = `${selectClassName} ${wardError ? selectErrorClassName : selectDefaultClassName}`.trim();

  return (
    <>
      <Input
        type="text"
        placeholder={streetPlaceholder}
        error={streetError}
        {...streetInputProps}
      />

      <div className={wrapperClassName}>
        <div className="flex flex-col">
          <select className={cityClassName} {...citySelectProps}>
            <option value="">{cityPlaceholder}</option>
            {provinces.map((province) => (
              <option key={province.code} value={province.code}>
                {province.name}
              </option>
            ))}
          </select>
          {cityError && <span className={errorTextClassName}>{cityError}</span>}
        </div>

        <div className="flex flex-col">
          <select className={wardClassName} {...wardSelectProps}>
            <option value="">{wardPlaceholder}</option>
            {wards.map((ward) => (
              <option key={ward.code} value={ward.code}>
                {ward.name}
              </option>
            ))}
          </select>
          {wardError && <span className={errorTextClassName}>{wardError}</span>}
        </div>
      </div>
    </>
  );
}
