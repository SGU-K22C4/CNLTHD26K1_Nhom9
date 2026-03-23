import { useState, useMemo } from 'react'
import { FILTER_OPTIONS } from '../data/mockProducts'

/* ─── Design tokens ────────────────────────────────────────── */
const PRIMARY = '#5A6D57'        // Primary-600
const NEUTRAL_GRAY = '#CBCBCB'   // Neutral Gray

/* ─── Size label map (Figma spec) ──────────────────────────── */
const SIZE_LABELS = {
  XS: 'XS / US (0-4)',
  S:  'S / US (4-6)',
  M:  'M / US (6-10)',
  L:  'L / US (10-14)',
  XL: 'XL / US (12-16)',
  XXL:'XXL / US (16+)',
}

/* ─── Tag close icon (14×14) ────────────────────────────────── */
function TagXIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
      <path d="M11 3L3 11M3 3L11 11" stroke="#202020" strokeWidth="1.4" strokeLinecap="round" />
    </svg>
  )
}

/* ─── SVG Icons ─────────────────────────────────────────────── */
function PlusIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
      <path d="M7 1V13M1 7H13" stroke="white" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  )
}

function MinusIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
      <path d="M1 7H13" stroke={PRIMARY} strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  )
}

function XIcon({ color = '#202020' }) {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M18 6L6 18M6 6L18 18" stroke={color} strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  )
}

/* ─── Square checkbox ────────────────────────────────────────── */
function SquareCheckbox({ checked }) {
  return (
    <span
      className="flex-shrink-0 w-4 h-4 flex items-center justify-center"
      style={{
        backgroundColor: checked ? PRIMARY : '#FFFFFF',
        border: `1px solid ${checked ? PRIMARY : NEUTRAL_GRAY}`,
      }}
    >
      {checked && (
        <svg width="9" height="7" viewBox="0 0 9 7" fill="none">
          <path
            d="M1 3.5L3.5 6L8 1"
            stroke="white"
            strokeWidth="1.4"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      )}
    </span>
  )
}

/* ─── Single accordion section ───────────────────────────────── */
function FilterAccordion({ label, expanded, onToggle, children }) {
  return (
    <div
      style={
        expanded
          ? { border: `1px solid ${NEUTRAL_GRAY}` }
          : {}
      }
    >
      {/* Header bar */}
      <button
        onClick={onToggle}
        className="w-full flex items-center justify-between px-4 h-[44px] text-[13px] font-medium tracking-[0.08em] uppercase transition-colors"
        style={{
          backgroundColor: expanded ? '#FFFFFF' : PRIMARY,
          color: expanded ? PRIMARY : '#FFFFFF',
          borderBottom: expanded ? `1px solid ${NEUTRAL_GRAY}` : 'none',
        }}
      >
        <span>{label}</span>
        {expanded ? <MinusIcon /> : <PlusIcon />}
      </button>

      {/* Expanded content */}
      {expanded && (
        <div className="px-4 py-4">
          {children}
        </div>
      )}
    </div>
  )
}

/* ─── Checkbox row (Sort / Size / Collection / Fabric) ─────── */
function CheckRow({ label, checked, onClick }) {
  return (
    <button
      onClick={onClick}
      className="flex items-center gap-3 w-full text-left py-[9px]"
    >
      <SquareCheckbox checked={checked} />
      <span className="text-[13px] text-[#202020] leading-none">{label}</span>
    </button>
  )
}

/* ─── Active Filter Tags + controls ─────────────────────────── */
function ActiveFilterTags({ tags, onClearAll }) {
  if (tags.length === 0) return null

  return (
    <div className="mb-4" style={{ boxSizing: 'border-box' }}>
      {/* ── Tag chips ────────────────────────────────────── */}
      <div className="flex flex-col gap-2 mb-4">
        {tags.map((tag) => (
          <span
            key={tag.id}
            className="inline-flex items-center justify-between gap-2 px-3 py-2 text-[14px] font-medium text-[#202020] w-fit min-w-[120px]"
            style={{ backgroundColor: NEUTRAL_GRAY, boxSizing: 'border-box' }}
          >
            <span>{tag.label}</span>
            <button
              onClick={tag.onRemove}
              aria-label={`Remove ${tag.label}`}
              className="flex items-center justify-center flex-shrink-0 hover:opacity-60 transition-opacity"
            >
              <TagXIcon />
            </button>
          </span>
        ))}
      </div>

      {/* ── Controls row ─────────────────────────────────── */}
      {/* Section padding: exactly 16px */}
      <div
        className="flex items-center gap-2"
        style={{ padding: '16px 0', boxSizing: 'border-box' }}
      >
        {/* Clear All Filters — bodyLG: 18px / Montserrat / weight 400 / capitalize / underline */}
        <button
          onClick={onClearAll}
          className="underline underline-offset-2 hover:opacity-70 transition-opacity whitespace-nowrap capitalize"
          style={{
            fontFamily: 'Montserrat, sans-serif',
            fontSize: '18px',
            lineHeight: '180%',
            fontWeight: 400,
            color: '#202020',
          }}
        >
          Clear All Filters
        </button>

        {/* APPLIED FILTERS — exact layer specs */}
        <button
          onClick={onClearAll}
          className="uppercase tracking-wide transition-opacity hover:opacity-90"
          style={{
            display: 'flex',
            height: '40px',
            padding: '0 16px',
            justifyContent: 'center',
            alignItems: 'center',
            gap: '4px',
            flex: '1 0 0',
            backgroundColor: PRIMARY,
            color: '#FFFFFF',
            fontFamily: 'Montserrat, sans-serif',
            fontSize: '13px',
            fontWeight: 600,
            letterSpacing: '0.1em',
            boxSizing: 'border-box',
          }}
        >
          Applied Filters
        </button>
      </div>
    </div>
  )
}

/* ─── Main component ─────────────────────────────────────────── */
export default function FilterSidebar({
  filters,
  setFilter,
  toggleArrayFilter,
  clearFilters,
  hasActiveFilters,
}) {
  /* Desktop sidebar sections */
  const [openSections, setOpenSections] = useState({
    sortBy: false,
    size: false,
    color: false,
    collection: false,
    fabric: false,
  })

  /* Mobile overlay open state */
  const [mobileOpen, setMobileOpen] = useState(false)

  const toggle = (key) =>
    setOpenSections((prev) => ({ ...prev, [key]: !prev[key] }))

  /* ── Build active tags list ──────────────────────────────── */
  const activeTags = useMemo(() => {
    const tags = []

    // Sort By (only when not default)
    if (filters.sortBy && filters.sortBy !== 'featured') {
      const opt = FILTER_OPTIONS.sortBy.find((o) => o.value === filters.sortBy)
      if (opt) {
        tags.push({
          id: `sortBy-${filters.sortBy}`,
          label: opt.label,
          onRemove: () => setFilter('sortBy', 'featured'),
        })
      }
    }

    // Sizes
    ;(filters.sizes || []).forEach((s) => {
      tags.push({
        id: `size-${s}`,
        label: SIZE_LABELS[s] || s,
        onRemove: () => toggleArrayFilter('sizes', s),
      })
    })

    // Colors
    ;(filters.colors || []).forEach((hex) => {
      const c = FILTER_OPTIONS.colors.find((x) => x.value === hex)
      if (c) {
        tags.push({
          id: `color-${hex}`,
          label: c.label,
          onRemove: () => toggleArrayFilter('colors', hex),
        })
      }
    })

    // Collections
    ;(filters.collections || []).forEach((col) => {
      tags.push({
        id: `collection-${col}`,
        label: col,
        onRemove: () => toggleArrayFilter('collections', col),
      })
    })

    // Fabrics
    ;(filters.fabrics || []).forEach((fab) => {
      tags.push({
        id: `fabric-${fab}`,
        label: fab,
        onRemove: () => toggleArrayFilter('fabrics', fab),
      })
    })

    return tags
  }, [filters, setFilter, toggleArrayFilter])

  /* Shared accordion content by section key */
  const renderSection = (key, toggleFn, openState) => {
    switch (key) {
      case 'sortBy':
        return (
          <FilterAccordion
            label="Sort By"
            expanded={openState.sortBy}
            onToggle={() => toggleFn('sortBy')}
          >
            <div className="flex flex-col">
              {FILTER_OPTIONS.sortBy.map((opt) => (
                <CheckRow
                  key={opt.value}
                  label={opt.label}
                  checked={filters.sortBy === opt.value}
                  onClick={() => setFilter('sortBy', opt.value)}
                />
              ))}
            </div>
          </FilterAccordion>
        )

      case 'size':
        return (
          <FilterAccordion
            label="Size"
            expanded={openState.size}
            onToggle={() => toggleFn('size')}
          >
            <div className="flex flex-col">
              {FILTER_OPTIONS.sizes.map((s) => (
                <CheckRow
                  key={s}
                  label={SIZE_LABELS[s] || s}
                  checked={filters.sizes?.includes(s)}
                  onClick={() => toggleArrayFilter('sizes', s)}
                />
              ))}
            </div>
          </FilterAccordion>
        )

      case 'color':
        return (
          <FilterAccordion
            label="Color"
            expanded={openState.color}
            onToggle={() => toggleFn('color')}
          >
            <div className="flex flex-wrap gap-3 py-1">
              {FILTER_OPTIONS.colors.map((c) => {
                const active = filters.colors?.includes(c.value)
                const needsBorder = ['#A8D5E2', '#D2B48C', '#C0C0C0', '#FFFFFF'].includes(c.value)
                return (
                  <button
                    key={c.value}
                    onClick={() => toggleArrayFilter('colors', c.value)}
                    title={c.label}
                    aria-label={c.label}
                    aria-pressed={active}
                    className="w-7 h-7 rounded-full flex-shrink-0 transition-transform hover:scale-110"
                    style={{
                      backgroundColor: c.value,
                      border: needsBorder ? `1px solid ${NEUTRAL_GRAY}` : 'none',
                      outline: active ? `2px solid ${PRIMARY}` : '2px solid transparent',
                      outlineOffset: '2px',
                    }}
                  />
                )
              })}
            </div>
          </FilterAccordion>
        )

      case 'collection':
        return (
          <FilterAccordion
            label="Collection"
            expanded={openState.collection}
            onToggle={() => toggleFn('collection')}
          >
            <div className="flex flex-col">
              {FILTER_OPTIONS.collections.map((col) => (
                <CheckRow
                  key={col}
                  label={col}
                  checked={filters.collections?.includes(col)}
                  onClick={() => toggleArrayFilter('collections', col)}
                />
              ))}
            </div>
          </FilterAccordion>
        )

      case 'fabric':
        return (
          <FilterAccordion
            label="Fabric"
            expanded={openState.fabric}
            onToggle={() => toggleFn('fabric')}
          >
            <div className="flex flex-col">
              {FILTER_OPTIONS.fabrics.map((fab) => (
                <CheckRow
                  key={fab}
                  label={fab}
                  checked={filters.fabrics?.includes(fab)}
                  onClick={() => toggleArrayFilter('fabrics', fab)}
                />
              ))}
            </div>
          </FilterAccordion>
        )

      default:
        return null
    }
  }

  const SECTION_KEYS = ['sortBy', 'size', 'color', 'collection', 'fabric']

  /* ── Mobile overlay open/close state (mirrors desktop) ─── */
  const [mobileOpenSections, setMobileOpenSections] = useState({
    sortBy: false,
    size: true,      // Size open by default (matches screenshot)
    color: false,
    collection: true, // Collection open by default (matches screenshot)
    fabric: false,
  })

  const toggleMobile = (key) =>
    setMobileOpenSections((prev) => ({ ...prev, [key]: !prev[key] }))

  return (
    <>
      {/* ══════════════════════════════════════════════════
          DESKTOP SIDEBAR (md and above)
      ════════════════════════════════════════════════════ */}
      <aside className="hidden md:block w-full">
        {/* "Filters" heading */}
        <h2 className="text-[15px] font-semibold text-[#202020] mb-4 tracking-wide">
          Filters
        </h2>

        {/* Active filter tags + Clear All / Applied Filters */}
        <ActiveFilterTags tags={activeTags} onClearAll={clearFilters} />

        {/* Accordion sections — spaced 8px apart */}
        <div className="flex flex-col gap-2">
          {SECTION_KEYS.map((key) => (
            <div key={key}>
              {renderSection(key, toggle, openSections)}
            </div>
          ))}
        </div>
      </aside>

      {/* ══════════════════════════════════════════════════
          MOBILE — "Filters" trigger button (below md)
      ════════════════════════════════════════════════════ */}
      <div className="md:hidden flex items-center justify-between mb-4">
        <button
          onClick={() => setMobileOpen(true)}
          className="flex items-center gap-2 text-[13px] font-medium tracking-[0.06em] uppercase text-white px-5 h-[44px]"
          style={{ backgroundColor: PRIMARY }}
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M2 4H14M4 8H12M6 12H10" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
          </svg>
          Filters
          {hasActiveFilters && (
            <span className="ml-1 w-4 h-4 rounded-full bg-white text-[10px] font-bold flex items-center justify-center" style={{ color: PRIMARY }}>
              ·
            </span>
          )}
        </button>
      </div>

      {/* ══════════════════════════════════════════════════
          MOBILE OVERLAY (360 px layout)
      ════════════════════════════════════════════════════ */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 flex md:hidden">
          {/* Backdrop */}
          <div
            className="absolute inset-0 bg-black/30"
            onClick={() => setMobileOpen(false)}
          />

          {/* Panel — slides in from left, max 360 px wide */}
          <div
            className="relative flex flex-col bg-white w-full max-w-[360px] h-full overflow-hidden"
            style={{ boxSizing: 'border-box' }}
          >
            {/* ── Header ── */}
            <div className="flex items-center justify-between px-6 pt-8 pb-6">
              <h2 className="text-[24px] font-bold text-[#202020] leading-none">
                Filters
              </h2>
              <button
                onClick={() => setMobileOpen(false)}
                className="w-10 h-10 flex items-center justify-center"
                aria-label="Close filters"
              >
                <XIcon />
              </button>
            </div>

            {/* ── Scrollable body ── */}
            <div className="flex-1 overflow-y-auto px-6">
              {/* Active tags in mobile overlay */}
              <ActiveFilterTags tags={activeTags} onClearAll={clearFilters} />
              <div className="flex flex-col gap-2 pb-4">
                {SECTION_KEYS.map((key) => (
                  <div key={key}>
                    {renderSection(key, toggleMobile, mobileOpenSections)}
                  </div>
                ))}
              </div>
            </div>

            {/* ── Footer ── */}
            <div className="flex items-center gap-4 px-6 py-5 border-t border-[#E8E8E8]">
              {/* Clear Filter */}
              <button
                onClick={() => {
                  clearFilters()
                  setMobileOpenSections({
                    sortBy: false,
                    size: false,
                    color: false,
                    collection: false,
                    fabric: false,
                  })
                }}
                className="flex-1 h-[48px] text-[13px] font-medium text-[#202020] tracking-[0.06em] uppercase text-center"
              >
                Clear Filter
              </button>

              {/* Apply Filter */}
              <button
                onClick={() => setMobileOpen(false)}
                className="flex-1 h-[48px] text-[13px] font-medium text-white tracking-[0.06em] uppercase"
                style={{ backgroundColor: PRIMARY }}
              >
                Apply Filter
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
