import { useState } from 'react'
import Modal from '../../../shared/components/ui/Modal'

const BASE_SIZE_ORDER = ['XS', 'S', 'M', 'L', 'XL', 'XXL']

const TEXT = {
  EN: {
    sizeGuide: 'Size Guide',
    baseSize: 'Base Size (Height / Weight)',
    baseSizeHint: 'Use height or weight to pick the larger size before adjustments.',
    adjustments: 'Adjustment Rules (Body Measurements)',
    measurementsUsed: 'Measurements Used',
    betweenSizes: 'If your measurements are between sizes, choose the larger size for a more relaxed fit.',
    bottomSizeTitle: 'Bottom Size Chart',
    bottomSizeHint: 'Use waist, hip, and weight to choose size.',
    bottomColumns: {
      waist: 'Waist (cm)',
      hip: 'Hip (cm)',
      weight: 'Suggested weight (kg)',
    },
    columns: {
      size: 'Size',
      height: 'Height (cm)',
      weight: 'Weight (kg)',
      notes: 'Notes',
    },
    baseHeight: {
      XS: 'Use S base',
      S: '<= 158',
      M: 'Between S and L thresholds',
      L: '>= 172',
      XL: '>= 178',
      XXL: 'Use XL base',
    },
    baseWeight: {
      XS: 'Use S base',
      S: '<= 50',
      M: 'Between S and L thresholds',
      L: '>= 67',
      XL: '>= 75',
      XXL: 'Use XL base',
    },
    baseNote: {
      XS: 'Only when a -1 adjustment applies.',
      S: 'Triggered by height or weight.',
      M: 'Default size when no thresholds hit.',
      L: 'Triggered by height or weight.',
      XL: 'Triggered by height or weight.',
      XXL: 'Only when a +1 adjustment applies.',
    },
    gender: {
      male: 'Men',
      female: 'Women',
      unisex: 'Unisex',
    },
    garment: {
      TOP: 'Top',
      BOTTOM: 'Bottom',
      ONEPIECE: 'One-piece',
    },
    effectLabel: 'Effect',
    measurements: {
      height: 'Height',
      weight: 'Weight',
      chest: 'Chest',
      waist: 'Waist',
      hip: 'Hip',
    },
    close: 'Close',
    language: {
      en: 'EN',
      vi: 'VI',
    },
  },
  VI: {
    sizeGuide: 'H\u01B0\u1EDBng d\u1EABn size',
    baseSize: 'Size c\u01A1 b\u1EA3n (Chi\u1EC1u cao / C\u00E2n n\u1EB7ng)',
    baseSizeHint: 'D\u00F9ng chi\u1EC1u cao ho\u1EB7c c\u00E2n n\u1EB7ng \u0111\u1EC3 ch\u1ECDn size l\u1EDBn h\u01A1n tr\u01B0\u1EDBc khi \u0111i\u1EC1u ch\u1EC9nh.',
    adjustments: 'Quy t\u1EAFc \u0111i\u1EC1u ch\u1EC9nh (S\u1ED1 \u0111o)',
    measurementsUsed: 'S\u1ED1 \u0111o s\u1EED d\u1EE5ng',
    betweenSizes: 'N\u1EBFu s\u1ED1 \u0111o n\u1EB1m gi\u1EEFa 2 size, h\u00E3y ch\u1ECDn size l\u1EDBn h\u01A1n \u0111\u1EC3 m\u1EB7c tho\u1EA3i m\u00E1i.',
    bottomSizeTitle: 'B\u1EA3ng size qu\u1EA7n',
    bottomSizeHint: 'D\u00F9ng v\u00F2ng eo, v\u00F2ng h\u00F4ng v\u00E0 c\u00E2n n\u1EB7ng \u0111\u1EC3 ch\u1ECDn size.',
    bottomColumns: {
      waist: 'V\u00F2ng eo (cm)',
      hip: 'V\u00F2ng h\u00F4ng (cm)',
      weight: 'G\u1EE3i \u00FD c\u00E2n n\u1EB7ng (kg)',
    },
    columns: {
      size: 'Size',
      height: 'Chi\u1EC1u cao (cm)',
      weight: 'C\u00E2n n\u1EB7ng (kg)',
      notes: 'Ghi ch\u00FA',
    },
    baseHeight: {
      XS: 'D\u00F9ng m\u1ED1c S',
      S: '<= 158',
      M: 'Gi\u1EEFa m\u1ED1c S v\u00E0 L',
      L: '>= 172',
      XL: '>= 178',
      XXL: 'D\u00F9ng m\u1ED1c XL',
    },
    baseWeight: {
      XS: 'D\u00F9ng m\u1ED1c S',
      S: '<= 50',
      M: 'Gi\u1EEFa m\u1ED1c S v\u00E0 L',
      L: '>= 67',
      XL: '>= 75',
      XXL: 'D\u00F9ng m\u1ED1c XL',
    },
    baseNote: {
      XS: 'Ch\u1EC9 \u00E1p d\u1EE5ng khi c\u00F3 \u0111i\u1EC1u ch\u1EC9nh -1 size.',
      S: 'K\u00EDch ho\u1EA1t theo chi\u1EC1u cao ho\u1EB7c c\u00E2n n\u1EB7ng.',
      M: 'M\u1EB7c \u0111\u1ECBnh n\u1EBFu kh\u00F4ng ch\u1EA1m ng\u01B0\u1EE1ng.',
      L: 'K\u00EDch ho\u1EA1t theo chi\u1EC1u cao ho\u1EB7c c\u00E2n n\u1EB7ng.',
      XL: 'K\u00EDch ho\u1EA1t theo chi\u1EC1u cao ho\u1EB7c c\u00E2n n\u1EB7ng.',
      XXL: 'Ch\u1EC9 \u00E1p d\u1EE5ng khi c\u00F3 \u0111i\u1EC1u ch\u1EC9nh +1 size.',
    },
    gender: {
      male: 'Nam',
      female: 'N\u1EEF',
      unisex: 'Unisex',
    },
    garment: {
      TOP: '\u00C1o',
      BOTTOM: 'Qu\u1EA7n',
      ONEPIECE: '\u0110\u1EA7m',
    },
    effectLabel: 'Hi\u1EC7u \u1EE9ng',
    measurements: {
      height: 'Chi\u1EC1u cao',
      weight: 'C\u00E2n n\u1EB7ng',
      chest: 'V\u00F2ng ng\u1EF1c',
      waist: 'V\u00F2ng eo',
      hip: 'V\u00F2ng h\u00F4ng',
    },
    close: '\u0110\u00F3ng',
    language: {
      en: 'EN',
      vi: 'VI',
    },
  },
}

const ADJUSTMENT_RULES = {
  TOP: [
    {
      area: { EN: 'Chest', VI: 'V\u00F2ng ng\u1EF1c' },
      rule: { EN: '>= 100 cm', VI: '>= 100 cm' },
      effect: { EN: '+1 size', VI: '+1 size' },
    },
  ],
  BOTTOM: [
    {
      area: { EN: 'Waist or Hip', VI: 'Eo ho\u1EB7c h\u00F4ng' },
      rule: { EN: 'Waist >= 86 cm or Hip >= 102 cm', VI: 'Eo >= 86 cm ho\u1EB7c H\u00F4ng >= 102 cm' },
      effect: { EN: '+1 size', VI: '+1 size' },
    },
    {
      area: { EN: 'Waist and Hip', VI: 'Eo v\u00E0 h\u00F4ng' },
      rule: { EN: 'Waist <= 70 cm and Hip <= 90 cm', VI: 'Eo <= 70 cm v\u00E0 H\u00F4ng <= 90 cm' },
      effect: { EN: '-1 size', VI: '-1 size' },
    },
  ],
  ONEPIECE: [
    {
      area: { EN: 'Chest', VI: 'V\u00F2ng ng\u1EF1c' },
      rule: { EN: '>= 100 cm', VI: '>= 100 cm' },
      effect: { EN: '+1 size', VI: '+1 size' },
    },
    {
      area: { EN: 'Waist or Hip', VI: 'Eo ho\u1EB7c h\u00F4ng' },
      rule: { EN: 'Waist >= 86 cm or Hip >= 102 cm', VI: 'Eo >= 86 cm ho\u1EB7c H\u00F4ng >= 102 cm' },
      effect: { EN: '+1 size', VI: '+1 size' },
    },
    {
      area: { EN: 'Waist and Hip', VI: 'Eo v\u00E0 h\u00F4ng' },
      rule: { EN: 'Waist <= 70 cm and Hip <= 90 cm', VI: 'Eo <= 70 cm v\u00E0 H\u00F4ng <= 90 cm' },
      effect: { EN: '-1 size', VI: '-1 size' },
    },
  ],
}

const BOTTOM_TABLES = {
  MALE: {
    title: { EN: 'Men Pants', VI: 'Qu\u1EA7n nam' },
    sizeLabel: { EN: 'Size', VI: 'Size' },
    rows: [
      { size: '34', waist: '70 - 75', hip: '86 - 91', weight: '50 - 55' },
      { size: '36', waist: '76 - 81', hip: '92 - 96', weight: '55 - 63' },
      { size: '38', waist: '82 - 87', hip: '97 - 101', weight: '63 - 70' },
      { size: '40', waist: '88 - 93', hip: '102 - 106', weight: '70 - 78' },
      { size: '42', waist: '94 - 99', hip: '107 - 111', weight: '78 - 85' },
    ],
  },
  FEMALE: {
    title: { EN: 'Women / Skirt (EU 32 - 46)', VI: 'Qu\u1EA7n n\u1EEF / Ch\u00E2n v\u00E1y (Size 32 - 46)' },
    sizeLabel: { EN: 'Size (EU)', VI: 'Size (EU)' },
    rows: [
      { size: '32', waist: '58 - 62', hip: '84 - 88', weight: '40 - 45' },
      { size: '34', waist: '63 - 67', hip: '89 - 93', weight: '45 - 50' },
      { size: '36', waist: '68 - 72', hip: '94 - 98', weight: '50 - 55' },
      { size: '38', waist: '73 - 77', hip: '99 - 103', weight: '55 - 60' },
      { size: '40', waist: '78 - 83', hip: '104 - 109', weight: '60 - 67' },
      { size: '42', waist: '84 - 89', hip: '110 - 115', weight: '67 - 73' },
      { size: '44', waist: '90 - 95', hip: '116 - 120', weight: '73 - 80' },
      { size: '46', waist: '96 - 101', hip: '121 - 126', weight: '80 - 87' },
    ],
  },
}

const CATEGORY_RULES = [
  {
    keys: ['ao so mi', 'so mi', 'blouse'],
    label: 'Shirts / Blouse',
    garmentType: 'TOP',
    fitNote: 'Size up if you have broad shoulders or a large chest.',
    fitNoteVi: 'N\u00EAn t\u0103ng 1 size n\u1EBFu vai r\u1ED9ng ho\u1EB7c v\u00F2ng ng\u1EF1c l\u1EDBn.',
  },
  {
    keys: ['ao thun', 'ao phong', 't-shirt', 'tee'],
    label: 'T-shirt',
    garmentType: 'TOP',
    fitNote: 'True to size.',
    fitNoteVi: 'M\u1EB7c \u0111\u00FAng size.',
  },
  {
    keys: ['knitwear', 'ao len', 'cardigan'],
    label: 'Knitwear / Cardigan',
    garmentType: 'TOP',
    fitNote: 'True to size (stretch fabric).',
    fitNoteVi: 'M\u1EB7c \u0111\u00FAng size (v\u1EA3i co gi\u00E3n).',
  },
  {
    keys: ['blazer'],
    label: 'Blazer',
    garmentType: 'TOP',
    fitNote: 'Size up one for layering.',
    fitNoteVi: 'N\u00EAn t\u0103ng 1 size n\u1EBFu m\u1EB7c layer.',
  },
  {
    keys: ['ao khoac', 'outerwear', 'coat', 'jacket'],
    label: 'Outerwear',
    garmentType: 'TOP',
    fitNote: 'Size up one for layering.',
    fitNoteVi: 'N\u00EAn t\u0103ng 1 size n\u1EBFu m\u1EB7c layer.',
  },
  {
    keys: ['quan tay', 'trousers', 'pants'],
    label: 'Trousers',
    garmentType: 'BOTTOM',
    fitNote: 'Size up if you have large thighs or hips.',
    fitNoteVi: 'N\u00EAn t\u0103ng size n\u1EBFu \u0111\u00F9i ho\u1EB7c h\u00F4ng to.',
  },
  {
    keys: ['quan jeans', 'jeans', 'denim'],
    label: 'Jeans',
    garmentType: 'BOTTOM',
    fitNote: 'Size up if you have large thighs or hips.',
    fitNoteVi: 'N\u00EAn t\u0103ng size n\u1EBFu \u0111\u00F9i ho\u1EB7c h\u00F4ng to.',
  },
  {
    keys: ['chan vay', 'skirt'],
    label: 'Skirt',
    garmentType: 'BOTTOM',
    fitNote: 'Focus on waist and hip measurements for fit.',
    fitNoteVi: '\u01AFu ti\u00EAn s\u1ED1 \u0111o eo v\u00E0 h\u00F4ng \u0111\u1EC3 ch\u1ECDn size.',
  },
  {
    keys: ['vay', 'dam', 'dress'],
    label: 'Dress',
    garmentType: 'ONEPIECE',
    fitNote: 'Use both chest and waist/hip adjustments for fit.',
    fitNoteVi: 'D\u00F9ng c\u1EA3 v\u00F2ng ng\u1EF1c v\u00E0 eo/h\u00F4ng \u0111\u1EC3 \u0111i\u1EC1u ch\u1EC9nh size.',
  },
]

function normalizeText(value) {
  if (!value) return ''
  return value
    .toString()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
}

function resolveCategory(categoryName) {
  const normalized = normalizeText(categoryName)
  for (const rule of CATEGORY_RULES) {
    if (rule.keys.some((key) => normalized.includes(key))) {
      return rule
    }
  }

  return {
    label: categoryName || 'Apparel',
    garmentType: 'TOP',
    fitNote: 'Use the measurement rules below for fit adjustments.',
    fitNoteVi: 'D\u00F9ng c\u00E1c quy t\u1EAFc s\u1ED1 \u0111o b\u00EAn d\u01B0\u1EDBi \u0111\u1EC3 \u0111i\u1EC1u ch\u1EC9nh size.',
  }
}

function getGenderLabel(categoryGender, language) {
  const text = TEXT[language] || TEXT.EN
  if (categoryGender === 'MALE') return text.gender.male
  if (categoryGender === 'FEMALE') return text.gender.female
  return text.gender.unisex
}

function getBottomSections(categoryGender) {
  if (categoryGender === 'MALE') return [BOTTOM_TABLES.MALE]
  if (categoryGender === 'FEMALE') return [BOTTOM_TABLES.FEMALE]
  return [BOTTOM_TABLES.MALE, BOTTOM_TABLES.FEMALE]
}

export default function SizeGuideModal({
  isOpen,
  onClose,
  categoryName,
  categoryGender,
}) {
  const [language, setLanguage] = useState('EN')
  const resolved = resolveCategory(categoryName)
  const text = TEXT[language] || TEXT.EN
  const garmentLabel = text.garment[resolved.garmentType] || text.garment.TOP
  const genderLabel = getGenderLabel(categoryGender, language)
  const adjustments = ADJUSTMENT_RULES[resolved.garmentType] || ADJUSTMENT_RULES.TOP
  const fitNote = language === 'VI' ? resolved.fitNoteVi : resolved.fitNote
  const bottomSections = resolved.garmentType === 'BOTTOM'
    ? getBottomSections(categoryGender)
    : []

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      className="max-w-4xl w-full rounded-[20px] overflow-hidden max-h-[calc(100vh-8rem)] my-10"
      showClose={false}
    >
      <div className="relative overflow-hidden">
        <style>{`
          @keyframes sgFadeUp {
            from { opacity: 0; transform: translateY(8px); }
            to { opacity: 1; transform: translateY(0); }
          }
          .sg-fade-up { animation: sgFadeUp 240ms ease-out; }
        `}</style>

        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,_#F6F2EA_0%,_#FFFFFF_55%,_#F8F8F5_100%)]" />
        <button
          type="button"
          onClick={onClose}
          aria-label={text.close}
          className="absolute top-4 right-4 z-10 h-9 w-9 rounded-full border border-[#E3E3DE] bg-white/90 text-[14px] text-[#2C2C2C] hover:bg-white transition-colors"
        >
          X
        </button>
        <div className="relative max-h-[calc(100vh-10rem)] overflow-y-auto">
          <div className="relative p-6 md:p-8 pt-12 pl-10 sg-fade-up">
          <div className="flex flex-col gap-2">
            <div className="flex items-start justify-between gap-4">
              <div className="flex flex-col gap-2">
                <p className="text-[11px] tracking-[0.28em] uppercase text-[#6A6A6A]">
                  {text.sizeGuide}
                </p>
                <h2 className="text-[24px] md:text-[28px] font-serif text-[#1D1D1D]">
                  {resolved.label}
                </h2>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="px-3 py-1 text-[10px] tracking-[0.2em] uppercase bg-[#F5F6F3] text-[#2C2C2C] border border-[#E3E3DE] rounded-full">
                    {genderLabel}
                  </span>
                  <span className="px-3 py-1 text-[10px] tracking-[0.2em] uppercase bg-[#E9EFE7] text-[#395445] border border-[#CFE0D3] rounded-full">
                    {garmentLabel}
                  </span>
                </div>
                <p className="text-[13px] text-[#555] max-w-2xl">
                  {fitNote}
                </p>
              </div>

              <div className="flex items-center gap-2">
                <div className="flex items-center rounded-full border border-[#E3E3DE] bg-white/80 p-1">
                  <button
                    type="button"
                    onClick={() => setLanguage('EN')}
                    className={`px-3 py-1 text-[10px] tracking-[0.2em] uppercase rounded-full transition-colors ${
                      language === 'EN'
                        ? 'bg-[#5A6D57] text-white'
                        : 'text-[#666] hover:text-[#202020]'
                    }`}
                  >
                    {text.language.en}
                  </button>
                  <button
                    type="button"
                    onClick={() => setLanguage('VI')}
                    className={`px-3 py-1 text-[10px] tracking-[0.2em] uppercase rounded-full transition-colors ${
                      language === 'VI'
                        ? 'bg-[#5A6D57] text-white'
                        : 'text-[#666] hover:text-[#202020]'
                    }`}
                  >
                    {text.language.vi}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div className="mt-6 border-t border-[#E5E1D8]" />

          {resolved.garmentType === 'BOTTOM' ? (
            <section className="mt-5">
              <div className="flex flex-col gap-1 mb-3">
                <h3 className="text-[12px] tracking-[0.24em] uppercase text-[#2C2C2C]">
                  {text.bottomSizeTitle}
                </h3>
                <p className="text-[11px] text-[#7A7A7A]">
                  {text.bottomSizeHint}
                </p>
              </div>
              <div className="space-y-4">
                {bottomSections.map((section) => (
                  <div key={section.title.EN}>
                    <p className="text-[11px] tracking-[0.24em] uppercase text-[#6A6A6A] mb-2">
                      {section.title[language]}
                    </p>
                    <div className="overflow-x-auto">
                      <table className="w-full min-w-[520px] text-[12px]">
                        <thead>
                          <tr className="bg-[#F7F6F2] text-[#4A4A4A]">
                            <th className="text-left font-medium px-3 py-2 border-b border-[#E6E2D8]">{section.sizeLabel[language]}</th>
                            <th className="text-left font-medium px-3 py-2 border-b border-[#E6E2D8]">{text.bottomColumns.waist}</th>
                            <th className="text-left font-medium px-3 py-2 border-b border-[#E6E2D8]">{text.bottomColumns.hip}</th>
                            <th className="text-left font-medium px-3 py-2 border-b border-[#E6E2D8]">{text.bottomColumns.weight}</th>
                          </tr>
                        </thead>
                        <tbody>
                          {section.rows.map((row) => (
                            <tr key={`${section.title.EN}-${row.size}`} className="border-b border-[#EFEAE1]">
                              <td className="px-3 py-2 font-semibold text-[#202020]">{row.size}</td>
                              <td className="px-3 py-2 text-[#444]">{row.waist}</td>
                              <td className="px-3 py-2 text-[#444]">{row.hip}</td>
                              <td className="px-3 py-2 text-[#6A6A6A]">{row.weight}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          ) : (
            <section className="mt-5">
              <div className="flex flex-col gap-1 mb-3">
                <h3 className="text-[12px] tracking-[0.24em] uppercase text-[#2C2C2C]">
                  {text.baseSize}
                </h3>
                <p className="text-[11px] text-[#7A7A7A]">
                  {text.baseSizeHint}
                </p>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[640px] text-[12px]">
                  <thead>
                    <tr className="bg-[#F7F6F2] text-[#4A4A4A]">
                      <th className="text-left font-medium px-3 py-2 border-b border-[#E6E2D8]">{text.columns.size}</th>
                      <th className="text-left font-medium px-3 py-2 border-b border-[#E6E2D8]">{text.columns.height}</th>
                      <th className="text-left font-medium px-3 py-2 border-b border-[#E6E2D8]">{text.columns.weight}</th>
                      <th className="text-left font-medium px-3 py-2 border-b border-[#E6E2D8]">{text.columns.notes}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {BASE_SIZE_ORDER.map((size) => (
                      <tr key={size} className="border-b border-[#EFEAE1]">
                        <td className="px-3 py-2 font-semibold text-[#202020]">{size}</td>
                        <td className="px-3 py-2 text-[#444]">{text.baseHeight[size]}</td>
                        <td className="px-3 py-2 text-[#444]">{text.baseWeight[size]}</td>
                        <td className="px-3 py-2 text-[#6A6A6A]">{text.baseNote[size]}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          )}

          <section className="mt-6">
            <h3 className="text-[12px] tracking-[0.24em] uppercase text-[#2C2C2C] mb-3">
              {text.adjustments}
            </h3>
            <div className="grid gap-3 md:grid-cols-2">
              {adjustments.map((rule) => (
                <div
                  key={`${rule.area.EN}-${rule.rule.EN}`}
                  className="rounded-xl border border-[#E6E2D8] bg-white/80 p-3"
                >
                  <p className="text-[10px] tracking-[0.2em] uppercase text-[#7A7A7A]">
                    {rule.area[language]}
                  </p>
                  <p className="text-[13px] text-[#202020] mt-1">{rule.rule[language]}</p>
                  <p className="text-[11px] text-[#5A6D57] mt-1">
                    {text.effectLabel}: {rule.effect[language]}
                  </p>
                </div>
              ))}
            </div>
          </section>

          <section className="mt-6">
            <h3 className="text-[12px] tracking-[0.24em] uppercase text-[#2C2C2C] mb-2">
              {text.measurementsUsed}
            </h3>
            <div className="grid gap-2 text-[12px] text-[#555] md:grid-cols-2">
              <div className="flex items-center justify-between rounded-lg border border-[#EFEAE1] bg-white/70 px-3 py-2">
                <span>{text.measurements.height}</span>
                <span className="text-[#202020]">cm</span>
              </div>
              <div className="flex items-center justify-between rounded-lg border border-[#EFEAE1] bg-white/70 px-3 py-2">
                <span>{text.measurements.weight}</span>
                <span className="text-[#202020]">kg</span>
              </div>
              <div className="flex items-center justify-between rounded-lg border border-[#EFEAE1] bg-white/70 px-3 py-2">
                <span>{text.measurements.chest}</span>
                <span className="text-[#202020]">cm</span>
              </div>
              <div className="flex items-center justify-between rounded-lg border border-[#EFEAE1] bg-white/70 px-3 py-2">
                <span>{text.measurements.waist}</span>
                <span className="text-[#202020]">cm</span>
              </div>
              <div className="flex items-center justify-between rounded-lg border border-[#EFEAE1] bg-white/70 px-3 py-2">
                <span>{text.measurements.hip}</span>
                <span className="text-[#202020]">cm</span>
              </div>
            </div>
          </section>

          <p className="mt-5 text-[11px] text-[#7A7A7A]">
            {text.betweenSizes}
          </p>
        </div>
        </div>
      </div>
    </Modal>
  )
}
