# Chatbot Eval Scorecard

Dung file nay de cham tung scenario trong `chatbot-eval-scenarios.md`.

## Cach cham

- Moi tieu chi cham 0-2:
  - `0` = fail ro rang
  - `1` = dung mot phan / tam duoc
  - `2` = dat ky vong
- Tong diem moi scenario:
  - `0-4`: yeu
  - `5-8`: trung binh
  - `9-12`: kha
  - `13-16`: tot

## Tieu chi cham mac dinh

### 1. Intent Accuracy

- Bot hieu dung nhu cau chinh cua user khong
- Co drift sang category/dip mac khac khong

### 2. Clarification Quality

- Bot hoi dung cau can hoi khong
- Co hoi qua nhieu cung luc khong
- Co hoi lai thong tin da co khong

### 3. Recommendation Relevance

- Goi y co dung dip mac / style / category khong
- Goi y co an toan va de chot khong

### 4. Size / Fit Quality

- Co dung rule size/fit khong
- Co giai thich ly do chon size khong

### 5. Budget Handling

- Goi y co ton trong budget khong
- Neu budget khong du, co de xuat thay the hop ly khong

### 6. Sales Tone

- Giong stylist tre, gan gui, co gu
- Khong qua sales, khong qua may moc

### 7. Follow-up And Closing

- Biet khi nao nen hoi tiep, khi nao nen chot
- Neu user do du, co xu ly tiep duoc khong

### 8. Conciseness

- Text co gon khong
- Co lap lai card/catalog khong

## Mau bang cham scenario

```md
Scenario: H1 - Session dai office to close

- Intent Accuracy: 2
- Clarification Quality: 1
- Recommendation Relevance: 2
- Size / Fit Quality: 2
- Budget Handling: 2
- Sales Tone: 1
- Follow-up And Closing: 1
- Conciseness: 2

Tong: 13/16

Nhan xet:
- Manh:
- Yeu:
- Loi can fix:
```

## KPI tong hop sau moi vong test

- `Intent accuracy average`
- `Size-fit accuracy average`
- `Budget compliance average`
- `Conversation drift count`
- `Repeated-question count`
- `Good close-out count`

## Muc tieu tam thoi

- `>= 12/16` cho nhom scenario uu tien cao
- `>= 10/16` cho scenario con lai
- `drift count` giam sau moi phase
- `repeated-question count` giam ro o H1
