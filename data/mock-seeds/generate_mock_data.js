const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// 1. Load Product IDs
const productIds = fs.readFileSync('product_ids.txt', 'utf16le')
    .split('\n')
    .map(id => id.trim().replace(/\0/g, ''))
    .filter(id => id.length > 0 && id !== 'id'); // skip header if any

if (productIds.length === 0) {
    console.error("No product IDs found.");
    process.exit(1);
}

const numUsers = 10;
const firstNames = ["Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng"];
const lastNames = ["Anh", "Bảo", "Chi", "Duy", "Giang", "Hải", "Khánh", "Linh", "Minh", "Ngọc"];
const reviewContents = [
    "Sản phẩm rất đẹp, chất lượng tuyệt vời!",
    "Vải mát, mặc rất thoải mái. Giao hàng nhanh.",
    "Form dáng chuẩn, đúng như hình ảnh mô tả.",
    "Hơi rộng so với mình nhưng chất vải thì ok.",
    "Màu sắc bên ngoài đẹp hơn trong ảnh. Rất ưng ý!",
    "Giá cả hợp lý, đáng tiền. Sẽ ủng hộ shop thêm.",
    "Chất liệu cao cấp, đường may tỉ mỉ, rất đáng mua.",
    "Mặc lên rất tôn dáng. Đã giới thiệu cho bạn bè."
];

let sqlUsers = "USE fashion_user_db;\n";
let sqlOrders = "USE fashion_order_db;\n";
let sqlOrderItems = "USE fashion_order_db;\n";
let jsReviews = "db = db.getSiblingDB('fashion_review_db');\n";

let orderIdCounter = Math.floor(Date.now() / 1000);
let orderItemIdCounter = orderIdCounter * 10;

for (let i = 1; i <= numUsers; i++) {
    const userId = crypto.randomUUID();
    const fname = firstNames[Math.floor(Math.random() * firstNames.length)];
    const lname = lastNames[Math.floor(Math.random() * lastNames.length)];
    const email = `mockuser${i}_${Date.now()}@example.com`;
    const phone = `09${Math.floor(10000000 + Math.random() * 90000000)}`;

    // Insert User
    sqlUsers += `INSERT INTO users (id, email, password, first_name, last_name, phone_number, role, is_active, is_email_verified, created_at, full_name) VALUES ('${userId}', '${email}', 'hashedpassword', '${fname}', '${lname}', '${phone}', 'CUSTOMER', 1, 1, NOW(), '${fname} ${lname}');\n`;

    // Generate 2-5 orders for this user
    const numOrders = 2 + Math.floor(Math.random() * 4);
    for (let j = 0; j < numOrders; j++) {
        const orderId = orderIdCounter++;
        const orderNumber = `ORD-${Date.now()}-${orderId}`;
        const total = 500000 + Math.floor(Math.random() * 2000000);
        
        // Insert Order
        sqlOrders += `INSERT INTO orders (id, order_number, user_id, status, subtotal, total, created_at) VALUES (${orderId}, '${orderNumber}', '${userId}', 'DELIVERED', ${total}, ${total}, NOW() - INTERVAL ${Math.floor(Math.random()*30)} DAY);\n`;

        // Generate 1-3 items per order
        const numItems = 1 + Math.floor(Math.random() * 3);
        const usedProductsForThisOrder = [];
        for (let k = 0; k < numItems; k++) {
            const itemId = orderItemIdCounter++;
            const productId = productIds[Math.floor(Math.random() * productIds.length)];
            
            // Prevent duplicate product in same order review constraint
            if (usedProductsForThisOrder.includes(productId)) continue;
            usedProductsForThisOrder.push(productId);

            const qty = 1 + Math.floor(Math.random() * 2);
            const price = 250000 + Math.floor(Math.random() * 500000);
            
            // Insert Order Item
            sqlOrderItems += `INSERT INTO order_items (id, order_id, product_id, product_name, quantity, unit_price, total_price) VALUES (${itemId}, ${orderId}, '${productId}', 'Mock Product', ${qty}, ${price}, ${qty * price});\n`;

            // Insert Review (MongoDB)
            const star = 4 + Math.floor(Math.random() * 2); // 4 or 5 stars
            const content = reviewContents[Math.floor(Math.random() * reviewContents.length)];
            const reviewId = crypto.randomUUID();
            
            jsReviews += `db.reviews.insertOne({
                _id: ObjectId(),
                review_id: '${reviewId}',
                user_id: '${userId}',
                product_id: '${productId}',
                order_id: '${orderId}',
                star: ${star},
                title: 'Rất tuyệt vời',
                content: '${content}',
                images: [],
                is_visible: true,
                created_at: new Date(),
                updated_at: new Date(),
                _class: 'com.fashion.reviewservice.entity.Review'
            });\n`;
        }
    }
}

const outputDir = __dirname;

// Keep generated mock artifacts colocated with this script so seed assets stay grouped.
fs.writeFileSync(path.join(outputDir, 'mock_users.sql'), sqlUsers);
fs.writeFileSync(path.join(outputDir, 'mock_orders.sql'), sqlOrders + sqlOrderItems);
fs.writeFileSync(path.join(outputDir, 'mock_reviews.js'), jsReviews);

console.log("Mock data generated successfully!");
