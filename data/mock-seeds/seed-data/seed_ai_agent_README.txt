AI agent test seed

Files:
- seed_ai_agent_data.sql
- seed_ai_agent_reviews.mongo.js

Generated scope:
- 50 CUSTOMER users
- 150 delivered orders (3 orders per user)
- 150 reviews (3 reviews per user)
- loyalty wallets and point transactions consistent with the seeded orders and reviews
- 6 dedicated products for test traffic

Credentials:
- all generated users use password: Customer@123

Import order:
1. Import MySQL data:
   mysql -u root -p < seed_ai_agent_data.sql
2. Import Mongo review data:
   mongosh < seed_ai_agent_reviews.mongo.js

Note:
- review-service in this project uses MongoDB, so review data cannot be packed into pure SQL.
- user passwords are stored as plain text on purpose to match the project backward-compatibility login path in AuthService.
