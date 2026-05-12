# Tổng quan Kiến trúc Hệ thống: Kubernetes, Kong Gateway & Kafka

Tài liệu này tổng hợp lại cấu trúc và luồng dữ liệu của hệ thống, đặc biệt tập trung vào các điểm nhấn trong quá trình thiết lập hạ tầng với Kubernetes (k8s) trên DigitalOcean, quản lý DNS thông qua Cloudflare, cùng cơ chế định tuyến qua Kong Gateway và điều phối event bằng Kafka. Đây là tài liệu phục vụ cho mục đích thuyết trình.

---

## 1. Cấu trúc Triển khai (Infrastructure Setup)

### Kubernetes trên DigitalOcean & Cloudflare DNS
- **Cloud Provider:** DigitalOcean (DO) cung cấp Managed Kubernetes / Droplets giúp tối ưu hóa chi phí và đảm bảo độ trễ thấp.
- **DNS Management:** Cloudflare quản lý DNS (VD: `modimal.tranhuy.dev`), cung cấp Proxy, CDN và SSL/TLS encryption tự động. Các request từ người dùng sẽ đi qua hệ thống phân giải của Cloudflare trước khi đến hệ thống Cân bằng tải (Load Balancer) của DigitalOcean và đẩy vào K8s Cluster.
- **Microservices Deployment:** Các services (User, Product, Cart, Order, v.v.) và cơ sở dữ liệu (MySQL, Redis, Mongo, Kafka) được quy hoạch trong `namespace` riêng (`fashion`), sử dụng ConfigMap để quản lý environment linh hoạt.

### Các Điểm Mạnh Để Thuyết Trình:
*   **Bảo mật lớp ngoài tối đa:** Nhờ Proxy của Cloudflare, hệ thống ẩn được IP thật của Node server, chống DDoS hiệu quả từ Tầng 3 đến Tầng 7.
*   **Scalability (Khả năng mở rộng):** Triển khai k8s chuẩn giúp scale (nhân bản pod) cho những service có traffic cao một cách độc lập mà không ảnh hưởng tới toàn hệ thống.
*   **Tự động hóa phục hồi:** K8s cung cấp cơ chế tự động restart container (qua `liveness` và `readiness` probes), đảm bảo uptime 99.9%.

---

## 2. Luồng Định tuyến - Kong Gateway (DB-less Mode)

Trong hệ thống, Kong đóng vai trò là **Lớp cửa ngõ (API Gateway)** duy nhất.

### Chi tiết Luồng xử lý:
1. **Tiếp nhận Request:** HTTP requests từ Frontend tới qua Cloudflare -> K8s Ingress/LoadBalancer -> **Kong Gateway**.
2. **CORS & Rate-Limiting:** Kong chặn hoặc cho phép request theo cấu hình origins rõ ràng (VD: Cloudflare DNS, localhost) và áp dụng chính sách giới hạn số lượng request (`rate-limiting`) nhằm tránh tình trạng spam API.
3. **Authentication (Custom JWT-Auth):** 
   - Với các API công khai (như đọc sản phẩm, đăng nhập), Kong pass thẳng xuống backend.
   - Với các API cần quyền (như đặt hàng, thao tác giỏ hàng), hệ thống cấu trúc 1 custom Lua plugin (`jwt-auth`). Kong trực tiếp decode, verify bảo mật JWT token thông qua `JWT_SECRET`. Nếu hợp lệ, Kong bóc tách thông tin (như `X-User-Id`, `X-User-Email`) gắn vào Request Head rồi mới truyền xuống Microservice, giúp Microservices bên dưới **hoàn toàn phi trạng thái (stateless) và không phải lo xử lý decode JWT**.

### Các Điểm Mạnh Để Thuyết Trình:
*   **Kiến trúc DB-less (Không cần CSDL cho Kong):** Sử dụng `kong.yml` declarative config qua Kubernetes ConfigMap giúp tối ưu RAM, giảm rủi ro single point of failure (điểm chết duy nhất), deploy cực nhanh theo mô hình GitOps.
*   **Single Responsibility:** Các microservice ở trong không cần share chung `user-db` để xác thực hay viết code check token. Kong đã che chắn và bóc tách dữ liệu sạch, đảm bảo chuẩn kiến trúc microservices.

---

## 3. Điều phối Event & Bất đồng bộ với Kafka

Kafka giải quyết bài toán giao tiếp giữa các service đằng sau lưng hệ thống.

### Chi tiết Luồng xử lý:
- Kiến trúc **Event-Driven:** Các thao tác cần quy trình chuỗi (như: Đặt hàng thành công -> Trừ mã khuyến mãi -> Gửi Email thông báo) không giao tiếp trực tiếp qua REST/HTTP (Synchronous) mà đẩy chung một event lên Kafka.
- Cấu trúc sử dụng bản `confluentinc/cp-kafka:7.6.1` cài đặt dạng Raft (KRaft mode với broker kiêm controller, không cần gánh theo Zookeeper nặng nề).
- Có `Kafka-UI` đi kèm để giám sát real-time trực quan các Node, Topic, Messages và Consumers Group trên môi trường K8s.

### Các Điểm Mạnh Để Thuyết Trình:
*   **Loose Coupling (Quy trình lỏng lẻo):** Service đặt hàng ném sự kiện vào Kafka rồi kết thúc luôn, trả kết quả tức thì cho Client. Các bộ phận như trừ tồn kho, gửi mail, lưu lịch sử, sẽ nhặt sự kiện đó về xử lý nền (Background processing).
*   **Độ tin cậy & Retry:** Nếu quá trình gửi mail bị sập, thư trong Kafka không mất đi; khi service Email lên lại, nó sẽ đọc bù và gửi theo thứ tự. Tối đa hóa Transaction state bảo toàn dữ liệu.
*   **Bắt kịp xu hướng hiện đại:** Thay vì Zookeeper legacy, bản setup sử dụng KRaft giúp cụm Kafka boot nhanh hơn, ngốn ít phần cứng hơn, hoàn toàn phù hợp với resource của môi trường Cloud K8s cấp phát nhạy bén.

---

## Tổng kết Lợi ích Khung Setup

1. **Vận hành:** K8s kết hợp DigitalOcean mang tới một hệ thống Production-ready nhưng tối giản hóa resource management. Mọi thứ được định nghĩa thành file `.yaml` dưới dạng Code (IaC).
2. **An ninh:** Luồng đi từ Cloudflare (DDoS/CDN) -> Kong (RateLimit, JWT check) tạo thành cấu hình "Two-Layer Protection" kiên cố.
3. **Hiệu suất ngầm:** Kafka làm hậu phương giúp xả tải tức thì khỏi luồng HTTP Block, tăng đột biến lượng Concurrent Users cho ứng dụng Frontend/Mobile.