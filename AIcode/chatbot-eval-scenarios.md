# Chatbot Eval Scenarios

Tai lieu nay dung de test chatbot tu van ban hang theo cung mot bo case co dinh sau moi lan nang cap.

## Cach dung

1. Chay chatbot o moi truong dev on dinh.
2. Test tung scenario theo dung thu tu message.
3. Doi chieu voi ky vong:
   - bot nen hoi gi
   - bot nen goi y gi
   - bot nen tranh gi
4. Cham diem bang `chatbot-eval-scorecard.md`.
5. Ghi lai loi drift, lap cau hoi, sai dip mac, sai size, sai budget.

## Nhom A. Product Discovery

### Scenario A1 - Do di lam budget thap

- User: `mình cần đồ đi làm văn phòng, ngân sách tầm 1 triệu`
- Ky vong:
  - bot hoi toi da 1-2 y lam ro
  - uu tien hoi dang can ao hay ca bo
  - uu tien item office-safe
  - neu goi y combo, combo phai hop budget
- Khong nen:
  - dua ngay qua nhieu san pham
  - goi y do di choi/party
  - hoi lien 4-5 cau

### Scenario A2 - Nhu cau cu the theo category

- User: `mình cần áo đen đi làm dưới 700k`
- Ky vong:
  - bot giu nguyen context `ao + di lam + duoi 700k`
  - neu chua du thong tin, hoi loai ao truoc
  - suggestions neu co phai uu tien office-safe
- Khong nen:
  - nhay sang vay/chan vay
  - bo qua budget

### Scenario A3 - Khach mua lan dau hoi chung chung

- User: `shop có gì đẹp không`
- Ky vong:
  - bot uu tien hero products hoac hoi nhu cau mo rong
  - tone phai giong stylist, khong ban catalog
- Khong nen:
  - noi `shop co rat nhieu mau dep`
  - day user ra tu tu xem website

## Nhom B. Size And Fit

### Scenario B1 - Phan van giua 2 size ao

- User: `mình cao 163cm nặng 55kg, áo sơ mi nên chọn S hay M`
- Ky vong:
  - bot khong chot size vo dieu kien
  - bot giai thich range S-M
  - hoi them fit preference hoac vai/nguc neu can
  - neu chot thi co ly do
- Khong nen:
  - tra loi `ban mac M nhe` khong giai thich

### Scenario B2 - Size follow-up co context

- User:
  - `mình cần áo sơ mi trắng đi làm`
  - `size m`
  - `vai mình hơi rộng, thích mặc thoải mái hơn`
- Ky vong:
  - bot giu mach size, khong quay lai discovery tu dau
  - uu tien rule Zara cho so mi
  - neu phan van S/M thi nghieng ve size an toan hon
- Khong nen:
  - hoi lai dip mac
  - mat context `ao so mi`

### Scenario B3 - Quan va body shape

- User: `quần jean này mình đùi to thì nên lên size không`
- Ky vong:
  - bot biet quyen uu tien dui/mong cho jeans
  - neu la skinny thi goi y len 1-2 size
  - neu straight/wide thi uu tien eo + body shape
- Khong nen:
  - tra loi theo ao hoac vay

## Nhom C. Outfit And Styling

### Scenario C1 - Goi y combo di lam

- User: `mình muốn set đồ đi làm gọn, dễ phối`
- Ky vong:
  - bot de xuat 1 combo chinh + 1 combo thay the
  - uu tien mau trung tinh va item de mac
  - neu co cards thi text ngan gon
- Khong nen:
  - dua 8-10 mon cung luc

### Scenario C2 - Chuyen sang phong cach co diem nhan

- User:
  - `mẫu nào an toàn và dễ phối hơn`
  - `nếu muốn có điểm nhấn hơn thì sao`
- Ky vong:
  - bot nhan biet dich chuyen tu `safe` sang `statement`
  - ly do so sanh ro rang
- Khong nen:
  - lap lai cung mot goi y

### Scenario C3 - Theo mua

- User: `đi du lịch mùa hè thì nên mặc gì`
- Ky vong:
  - uu tien linen/cotton/vai nhe
  - combo va mau sac dung theo rule mua he
- Khong nen:
  - goi y trench/wool/denim nang

## Nhom D. Price Objection

### Scenario D1 - Khach che gia cao

- User:
  - `áo này 899k đắt quá`
- Ky vong:
  - bot giai thich gia tri truoc
  - sau do moi de xuat phuong an mem hon neu can
  - van giu positioning brand
- Khong nen:
  - ha gia vo toi va
  - push sale lo lieu

### Scenario D2 - Budget refinement

- User:
  - `mình cần áo đen đi làm`
  - `vậy thì dưới 2 triệu`
- Ky vong:
  - bot khong parse sai `vay`
  - van giu category dang theo duoi
  - ranking theo budget moi

## Nhom E. Compare And Decision

### Scenario E1 - Phan van 2 mau

- User: `mình phân vân 2 mẫu này, nên chốt mẫu nào`
- Ky vong:
  - bot so sanh theo dip mac, do versatile, de phoi, gia
  - ket thuc bang mot de xuat ro hon
- Khong nen:
  - noi 2 mau deu dep nhu nhau

### Scenario E2 - Khach noi de nghi them

- User: `thôi để mình nghĩ thêm đã`
- Ky vong:
  - bot follow-up mem
  - hoi khach dang lan tan gi: gia / size / dang
- Khong nen:
  - dung tai `ok ban nhe`

## Nhom F. Loyalty And Wishlist

### Scenario F1 - Wishlist review

- User: `mình có gì trong wishlist`
- Ky vong:
  - khong NPE
  - tra ve dung cards neu co
  - text ngan, khong lap catalog

### Scenario F2 - Loyalty

- User: `tôi còn bao nhiêu điểm thưởng`
- Ky vong:
  - tra ve dung loyalty context
  - neu phu hop, co the nhac loi ich ngan gon

## Nhom G. Gift And Special Cases

### Scenario G1 - Mua qua

- User: `mình muốn mua áo cho bạn gái nhưng không biết size`
- Ky vong:
  - bot hoi dang nguoi + style
  - de xuat item an toan de tang
  - uu tien form rong/co gian
- Khong nen:
  - chot size vo can cu

### Scenario G2 - Mac tre hon

- User: `mình muốn mặc trẻ hơn một chút`
- Ky vong:
  - bot de xuat doi item / fit / combo tre hon
  - khong goi y qua tre trau hoac sai persona

## Nhom H. End-to-End Session DAI

### Scenario H1 - Session dai office to close

- User 1: `mình cần áo đen đi làm dưới 2 triệu`
- User 2: `áo thun basic dáng lịch sự size m`
- User 3: `1m70, 65kg`
- User 4: `nếu an toàn hơn thì chọn gì`
- User 5: `nếu giá mềm hơn chút thì sao`
- User 6: `so sánh 2 mẫu đầu giúp mình`
- User 7: `mình lấy mẫu đầu`

- Ky vong:
  - bot giu context category + budget + size
  - khong hoi lai thong tin da co
  - chuyen duoc discovery -> size -> compare -> close
  - tone van tu nhien

## Muc do uu tien khi test

- Cao nhat:
  - B1, B2, D1, E1, H1
- Trung binh:
  - A1, A2, C1, C2, F1
- Duy tri:
  - G1, G2, F2
