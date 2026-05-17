# Chatbot Regression Checklist

Checklist nhanh sau moi lan sua bot.

## 1. Runtime Sanity

- Chatbot-service startup binh thuong
- Mongo knowledge ingest thanh cong
- Khong co NPE o wishlist / loyalty / compare
- FE chatbot page render on dinh
- Mini widget chat render on dinh

## 2. Core Chat Cases

- `mình cần áo đen đi làm dưới 700k`
- `áo thun basic dáng lịch sự size m`
- `1m70, 65kg`
- `vậy thì dưới 2 triệu`
- `mình có gì trong wishlist`
- `tôi còn bao nhiêu điểm thưởng`
- `mình phân vân 2 mẫu này`
- `áo này 899k đắt quá`

## 3. Khong Duoc Tua Lai

- Khong parse nham `vậy` thanh `váy`
- Khong lap lai ten + mau + size cua ca danh sach san pham trong bubble
- Khong hoi lai size/dip mac da co
- Khong roi vao fallback vo nghia
- Khong drift sang category khac

## 4. Analytics Events

- `product_click` duoc gui khi bam card trong chat
- `product_click` duoc gui khi bam `Latest Curated Picks`
- `compare_intent` duoc gui khi user noi y so sanh
- `view_more_products` duoc gui khi user xin them lua chon
- `add_to_cart_intent` duoc gui khi user noi muon lay/chot/thêm giỏ
- `add_to_cart_success` duoc gui khi them gio tu san pham den tu chatbot
- `checkout_submit` duoc gui khi tao order
- `order_success` duoc gui sau COD/VNPay thanh cong

## 5. Business Checks

- Office query uu tien item office-safe
- Summer query uu tien cotton/linen/vai nhe
- Size advice dung theo rule Zara
- Gia cao duoc xu ly bang value-first roi moi alternative
- Hero products chi uu tien khi phu hop nhu cau

## 6. Manual Notes

```md
Ngay test:
Moi truong:
Nguoi test:

Bug 1:
Bug 2:
Bug 3:

Scenario tot nhat:
Scenario do nhat:

De xuat phase tiep theo:
```
