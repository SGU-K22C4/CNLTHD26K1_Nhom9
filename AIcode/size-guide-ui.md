# Product Size Guide Plan (Implementation)

Create a size guide modal on the product page that uses existing category and size data from the system, and derives measurement guidance from the current SizeAdvisor rules plus business notes. This provides a per-category, gender-aware table with height, weight, and body measurements, with an English/Vietnamese (EN/VI) toggle, Vietnamese diacritics, gender-specific bottom charts (waist/hip/weight), and a close button.

## Steps
1. Phase 0 - Documentation: Keep this doc as the source of truth for size guide UI and logic.
2. Phase 1 - Data model: Inventory product category inputs available on the frontend (`categoryName`, `categoryGender`) and define a category-to-garment-type map (TOP, BOTTOM, DRESS/ONEPIECE) with per-category fit notes from the business rules doc.
3. Phase 1 - Data model: Translate SizeAdvisor thresholds (height/weight base sizing and chest/waist/hip adjustments) into a display-friendly size guide structure with explicit labels and ranges/thresholds for XS-XXL. Note that DB does not store measurement ranges; we derive them from the logic.
4. Phase 2 - UI component: Build a `SizeGuideModal` component using the shared Modal overlay; include header (category + gender), a visible close button, and an EN/VI language toggle.
5. Phase 2 - UI component: Use a responsive table layout with horizontal overflow for small screens, and vertical scrolling when the modal content is too long. Add extra top spacing so the table does not collide with the header area. Widen the modal on desktop for better table readability.
6. Phase 2 - UI component: For BOTTOM categories, replace the base size table with gender-specific charts:
	- Men pants: sizes 34-42 with waist/hip/weight ranges.
	- Women/skirt (EU 32-46): sizes 32-46 with waist/hip/weight ranges.
7. Phase 2 - UI component: Add a small rules section under the table to explain adjustments (e.g., chest >= 100cm -> +1 size for TOP; waist/hip thresholds for BOTTOM) and category-specific fit guidance (true to size, size up for layering, etc.).
8. Phase 3 - Integration: Wire the existing "Size Guide" link on the product detail page to open the modal; pass in `product.categoryName` and `product.categoryGender`; add graceful fallback when a category is unmapped.
9. Phase 4 - Review: Validate EN/VI copy with Vietnamese diacritics, table layout, and mobile behavior; confirm the mappings cover all current categories and that the fallback messaging is clear.

## Relevant Files
- backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/impl/SizeAdvisorServiceImpl.java
- backend/services/chatbot-service/src/main/java/com/fashion/chatbotservice/service/SizeAdvisorService.java
- AIcode/nghiep-vu-tu-van-ban-hang-fashion-chatbot.md
- ecommerce-frontend/src/modules/product/pages/ProductDetailPage.jsx
- ecommerce-frontend/src/shared/components/ui/Modal.jsx
- ecommerce-frontend/src/modules/product/services/productService.js

## Verification
1. Open a product detail page, click "Size Guide", and confirm the modal opens and closes (backdrop + X).
2. Check a TOP category (e.g., shirts) and a BOTTOM category (e.g., jeans) to ensure correct columns and rules show.
3. Switch between EN/VI and confirm labels and notes change with proper Vietnamese diacritics.
4. Switch between TOP/BOTTOM to confirm the bottom table replaces the base size table.
5. On desktop, confirm the size guide modal is slightly wider for easier table scanning.
6. Switch between male/female categories and confirm the correct bottom chart appears.
7. Test on mobile width to ensure table scrolls horizontally and the modal scrolls vertically when content is long.

## Decisions
- Show size guide in a modal triggered by the existing "Size Guide" link.
- Provide a visible close (X) button in the modal header.
- Scope size guide per category, gender-aware.
- Derive guidance from SizeAdvisor rules + business sizing notes (no new DB table).
- Provide an EN/VI toggle inside the modal.
- Keep the modal content scrollable for smaller screens.
- Use gender-specific bottom charts with waist/hip/weight columns.

## Further Considerations
If you want strict numeric ranges per size (not just thresholds/adjustment rules), define them explicitly in config once official ranges are available.
