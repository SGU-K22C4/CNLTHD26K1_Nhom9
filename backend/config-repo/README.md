# Hướng Dẫn Cấu Hình (Config Repo) cho Team Dev

Đây là hệ thống quản lý config tập trung bằng **Spring Cloud Config Server**. Thay vì mỗi service tự ôm một mớ file `.yml` và `.env` lằng nhằng, giờ đây toàn bộ cấu hình nằm ở thư mục này!

## 1. Kiến Trúc Hoạt Động (Spring Cloud Config)
- `config-service`: Đóng vai trò là "Tổng Đài". Cổng mặc định: `8888`.
- `user-service`, `product-service`...: Đóng vai trò "Chi nhánh". Khi khởi động, chúng chỉ có đúng 1 dòng code duy nhất là `import: "optional:configserver:http://localhost:8888"`.
- Khi "Chi nhánh" khởi động, nó chạy tới "Tổng Đài" xin cấu hình. "Tổng Đài" sẽ vào thư mục `config-repo` này, nhặt file `application.yml` (dùng chung) và `[ten-service].yml` gửi về cho nó.

## 2. Dev Mới Lấy Code Về Cần Làm Gì?

### A. Chuẩn bị Hạ Tầng (Database rỗng)
1. Mở Terminal, đi vào dự án: `cd docker`
2. Chạy lệnh: `docker-compose up -d mysql redis mongo kong` (KHÔNG build code Java bằng Docker ở Local).
3. (Hoặc nếu máy cấu hình yếu) Bạn có thể dùng XAMPP / MongoDB Compass mở ở local và tự tạo nhanh các Database: `fashion_user_db`, `fashion_product_db`,... có tên khớp với trong thư mục này.

### B. Khởi động Source Code 
Quy tắc vàng: **LUÔN BẬT `ConfigServiceApplication` (Port 8888) TRƯỚC TIÊN.**
1. Khởi động Config Service. - chuột phải bấm run tại file ConfigServiceApplication.java
2. Khởi động các Service muốn làm việc (ví dụ `UserServiceApplication` hoặc `ProductServiceApplication`...).
3. Flyway Database Migration sẽ tự động tạo bảng vào Database rỗng bạn vừa dựng.

## 3. Nếu Muốn Sửa Bí Mật (Secret Key) Thì Sao?
> **Lưu ý Quan Trọng:** Tuyệt đối KHÔNG SỬA cứng mật khẩu thật của Production vào các file `.yml` này để push code lên Git!

Ở dạng cấu hình hiện hành, các file như `application.yml` đang được set giá trị Default (dự phòng) qua biến môi trường. Ví dụ: `${JWT_SECRET:chuoi-bi-mat}`.
- Nếu bạn code ở máy Local, các giá trị dự phòng trong file này (như `localhost:3306`) đã đủ để nó chạy **TRƠN TRU NGAY TỨC KHẮC** mà không cần sửa gì thêm.
- **Để Test API cần Key Thật (như VNPay, OpenAI GPT, Gửi Mail)**: Do các hàm này gọi API tốn tiền/sandbox riêng tư, hãy tự thiết lập biến Environment Variable cục bộ trên máy tính hoặc config trực tiếp vào IDE (phần Edit Configurations trong IntelliJ/Eclipse) các biến:
  - `OPENAI_API_KEY`: Mã ChatGPT thực tế
  - `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`: Key test thanh toán
  - `MAIL_USERNAME`, `MAIL_PASSWORD`: Mật khẩu ứng dụng Gmail

## 4.Cách cài .env để add các biến môi trường tại backend 
**Tạo file .env trong thư mục backend :** dựa theo file .env.example tại backend\.env.example
các bạn tạo 1 file .env copy sườn file .env.example vào và sửa lại cho phù hợp với máy của mình , các biến tài nguyên nào mình tag note là hãy sửa thì các bạn tạo và paste vào thay thế nhé. File .env này sẽ được Spring Boot đọc khi khởi động và truyền vào các service thông qua các file .yml ( hiểu đơn giản là như vậy). 


## 5. Chuyển Đổi Môi Trường (Local -> Production)
Khi đem server triển khai lên K8s (hoặc VPS Cloud), DevOps chỉ cần nạp các biến môi trường (Ví dụ `SPRING_DATASOURCE_PASSWORD=xyz`) vào container. Spring Boot sẽ tự động bỏ qua giá trị cấu hình local trong các file này và ăn theo mật khẩu production xịn!

Mọi câu hỏi, vui lòng liên hệ Leader! Happy Coding! 🚀

## cặp lệnh xịn khi build mvn spring-boot:run