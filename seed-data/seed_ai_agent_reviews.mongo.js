// AI agent seed reviews for fashion_review_db
// Run with: mongosh < seed_ai_agent_reviews.mongo.js
const dbRef = db.getSiblingDB("fashion_review_db");
const docs = [
  {
    "_id": "seed-review-01-1",
    "review_id": "seed-review-01-1",
    "user_id": "seed-user-001",
    "product_id": "seed-prod-ai-001",
    "order_id": "10001",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 01, order ORD-AI-010001.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-09T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-09T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-01-2",
    "review_id": "seed-review-01-2",
    "user_id": "seed-user-001",
    "product_id": "seed-prod-ai-002",
    "order_id": "10002",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 01, order ORD-AI-010002.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-10T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-10T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-01-3",
    "review_id": "seed-review-01-3",
    "user_id": "seed-user-001",
    "product_id": "seed-prod-ai-003",
    "order_id": "10003",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 01, order ORD-AI-010003.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-11T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-11T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-02-1",
    "review_id": "seed-review-02-1",
    "user_id": "seed-user-002",
    "product_id": "seed-prod-ai-002",
    "order_id": "10004",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 02, order ORD-AI-010004.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-10T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-10T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-02-2",
    "review_id": "seed-review-02-2",
    "user_id": "seed-user-002",
    "product_id": "seed-prod-ai-003",
    "order_id": "10005",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 02, order ORD-AI-010005.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-11T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-11T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-02-3",
    "review_id": "seed-review-02-3",
    "user_id": "seed-user-002",
    "product_id": "seed-prod-ai-004",
    "order_id": "10006",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 02, order ORD-AI-010006.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-12T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-12T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-03-1",
    "review_id": "seed-review-03-1",
    "user_id": "seed-user-003",
    "product_id": "seed-prod-ai-003",
    "order_id": "10007",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 03, order ORD-AI-010007.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-11T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-11T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-03-2",
    "review_id": "seed-review-03-2",
    "user_id": "seed-user-003",
    "product_id": "seed-prod-ai-004",
    "order_id": "10008",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 03, order ORD-AI-010008.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-12T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-12T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-03-3",
    "review_id": "seed-review-03-3",
    "user_id": "seed-user-003",
    "product_id": "seed-prod-ai-005",
    "order_id": "10009",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 03, order ORD-AI-010009.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-13T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-13T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-04-1",
    "review_id": "seed-review-04-1",
    "user_id": "seed-user-004",
    "product_id": "seed-prod-ai-004",
    "order_id": "10010",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 04, order ORD-AI-010010.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-12T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-12T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-04-2",
    "review_id": "seed-review-04-2",
    "user_id": "seed-user-004",
    "product_id": "seed-prod-ai-005",
    "order_id": "10011",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 04, order ORD-AI-010011.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-13T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-13T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-04-3",
    "review_id": "seed-review-04-3",
    "user_id": "seed-user-004",
    "product_id": "seed-prod-ai-006",
    "order_id": "10012",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 04, order ORD-AI-010012.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-14T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-14T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-05-1",
    "review_id": "seed-review-05-1",
    "user_id": "seed-user-005",
    "product_id": "seed-prod-ai-005",
    "order_id": "10013",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 05, order ORD-AI-010013.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-13T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-13T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-05-2",
    "review_id": "seed-review-05-2",
    "user_id": "seed-user-005",
    "product_id": "seed-prod-ai-006",
    "order_id": "10014",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 05, order ORD-AI-010014.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-14T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-14T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-05-3",
    "review_id": "seed-review-05-3",
    "user_id": "seed-user-005",
    "product_id": "seed-prod-ai-001",
    "order_id": "10015",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 05, order ORD-AI-010015.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-15T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-15T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-06-1",
    "review_id": "seed-review-06-1",
    "user_id": "seed-user-006",
    "product_id": "seed-prod-ai-006",
    "order_id": "10016",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 06, order ORD-AI-010016.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-14T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-14T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-06-2",
    "review_id": "seed-review-06-2",
    "user_id": "seed-user-006",
    "product_id": "seed-prod-ai-001",
    "order_id": "10017",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 06, order ORD-AI-010017.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-15T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-15T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-06-3",
    "review_id": "seed-review-06-3",
    "user_id": "seed-user-006",
    "product_id": "seed-prod-ai-002",
    "order_id": "10018",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 06, order ORD-AI-010018.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-16T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-16T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-07-1",
    "review_id": "seed-review-07-1",
    "user_id": "seed-user-007",
    "product_id": "seed-prod-ai-001",
    "order_id": "10019",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 07, order ORD-AI-010019.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-15T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-15T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-07-2",
    "review_id": "seed-review-07-2",
    "user_id": "seed-user-007",
    "product_id": "seed-prod-ai-002",
    "order_id": "10020",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 07, order ORD-AI-010020.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-16T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-16T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-07-3",
    "review_id": "seed-review-07-3",
    "user_id": "seed-user-007",
    "product_id": "seed-prod-ai-003",
    "order_id": "10021",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 07, order ORD-AI-010021.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-17T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-17T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-08-1",
    "review_id": "seed-review-08-1",
    "user_id": "seed-user-008",
    "product_id": "seed-prod-ai-002",
    "order_id": "10022",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 08, order ORD-AI-010022.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-16T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-16T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-08-2",
    "review_id": "seed-review-08-2",
    "user_id": "seed-user-008",
    "product_id": "seed-prod-ai-003",
    "order_id": "10023",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 08, order ORD-AI-010023.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-17T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-17T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-08-3",
    "review_id": "seed-review-08-3",
    "user_id": "seed-user-008",
    "product_id": "seed-prod-ai-004",
    "order_id": "10024",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 08, order ORD-AI-010024.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-18T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-18T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-09-1",
    "review_id": "seed-review-09-1",
    "user_id": "seed-user-009",
    "product_id": "seed-prod-ai-003",
    "order_id": "10025",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 09, order ORD-AI-010025.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-17T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-17T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-09-2",
    "review_id": "seed-review-09-2",
    "user_id": "seed-user-009",
    "product_id": "seed-prod-ai-004",
    "order_id": "10026",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 09, order ORD-AI-010026.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-18T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-18T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-09-3",
    "review_id": "seed-review-09-3",
    "user_id": "seed-user-009",
    "product_id": "seed-prod-ai-005",
    "order_id": "10027",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 09, order ORD-AI-010027.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-19T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-19T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-10-1",
    "review_id": "seed-review-10-1",
    "user_id": "seed-user-010",
    "product_id": "seed-prod-ai-004",
    "order_id": "10028",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 10, order ORD-AI-010028.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-18T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-18T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-10-2",
    "review_id": "seed-review-10-2",
    "user_id": "seed-user-010",
    "product_id": "seed-prod-ai-005",
    "order_id": "10029",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 10, order ORD-AI-010029.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-19T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-19T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-10-3",
    "review_id": "seed-review-10-3",
    "user_id": "seed-user-010",
    "product_id": "seed-prod-ai-006",
    "order_id": "10030",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 10, order ORD-AI-010030.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-20T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-20T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-11-1",
    "review_id": "seed-review-11-1",
    "user_id": "seed-user-011",
    "product_id": "seed-prod-ai-005",
    "order_id": "10031",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 11, order ORD-AI-010031.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-19T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-19T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-11-2",
    "review_id": "seed-review-11-2",
    "user_id": "seed-user-011",
    "product_id": "seed-prod-ai-006",
    "order_id": "10032",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 11, order ORD-AI-010032.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-20T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-20T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-11-3",
    "review_id": "seed-review-11-3",
    "user_id": "seed-user-011",
    "product_id": "seed-prod-ai-001",
    "order_id": "10033",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 11, order ORD-AI-010033.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-21T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-21T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-12-1",
    "review_id": "seed-review-12-1",
    "user_id": "seed-user-012",
    "product_id": "seed-prod-ai-006",
    "order_id": "10034",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 12, order ORD-AI-010034.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-20T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-20T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-12-2",
    "review_id": "seed-review-12-2",
    "user_id": "seed-user-012",
    "product_id": "seed-prod-ai-001",
    "order_id": "10035",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 12, order ORD-AI-010035.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-21T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-21T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-12-3",
    "review_id": "seed-review-12-3",
    "user_id": "seed-user-012",
    "product_id": "seed-prod-ai-002",
    "order_id": "10036",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 12, order ORD-AI-010036.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-22T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-22T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-13-1",
    "review_id": "seed-review-13-1",
    "user_id": "seed-user-013",
    "product_id": "seed-prod-ai-001",
    "order_id": "10037",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 13, order ORD-AI-010037.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-21T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-21T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-13-2",
    "review_id": "seed-review-13-2",
    "user_id": "seed-user-013",
    "product_id": "seed-prod-ai-002",
    "order_id": "10038",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 13, order ORD-AI-010038.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-22T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-22T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-13-3",
    "review_id": "seed-review-13-3",
    "user_id": "seed-user-013",
    "product_id": "seed-prod-ai-003",
    "order_id": "10039",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 13, order ORD-AI-010039.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-23T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-23T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-14-1",
    "review_id": "seed-review-14-1",
    "user_id": "seed-user-014",
    "product_id": "seed-prod-ai-002",
    "order_id": "10040",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 14, order ORD-AI-010040.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-22T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-22T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-14-2",
    "review_id": "seed-review-14-2",
    "user_id": "seed-user-014",
    "product_id": "seed-prod-ai-003",
    "order_id": "10041",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 14, order ORD-AI-010041.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-23T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-23T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-14-3",
    "review_id": "seed-review-14-3",
    "user_id": "seed-user-014",
    "product_id": "seed-prod-ai-004",
    "order_id": "10042",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 14, order ORD-AI-010042.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-24T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-24T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-15-1",
    "review_id": "seed-review-15-1",
    "user_id": "seed-user-015",
    "product_id": "seed-prod-ai-003",
    "order_id": "10043",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 15, order ORD-AI-010043.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-23T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-23T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-15-2",
    "review_id": "seed-review-15-2",
    "user_id": "seed-user-015",
    "product_id": "seed-prod-ai-004",
    "order_id": "10044",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 15, order ORD-AI-010044.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-24T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-24T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-15-3",
    "review_id": "seed-review-15-3",
    "user_id": "seed-user-015",
    "product_id": "seed-prod-ai-005",
    "order_id": "10045",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 15, order ORD-AI-010045.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-25T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-25T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-16-1",
    "review_id": "seed-review-16-1",
    "user_id": "seed-user-016",
    "product_id": "seed-prod-ai-004",
    "order_id": "10046",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 16, order ORD-AI-010046.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-24T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-24T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-16-2",
    "review_id": "seed-review-16-2",
    "user_id": "seed-user-016",
    "product_id": "seed-prod-ai-005",
    "order_id": "10047",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 16, order ORD-AI-010047.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-25T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-25T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-16-3",
    "review_id": "seed-review-16-3",
    "user_id": "seed-user-016",
    "product_id": "seed-prod-ai-006",
    "order_id": "10048",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 16, order ORD-AI-010048.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-26T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-26T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-17-1",
    "review_id": "seed-review-17-1",
    "user_id": "seed-user-017",
    "product_id": "seed-prod-ai-005",
    "order_id": "10049",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 17, order ORD-AI-010049.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-25T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-25T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-17-2",
    "review_id": "seed-review-17-2",
    "user_id": "seed-user-017",
    "product_id": "seed-prod-ai-006",
    "order_id": "10050",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 17, order ORD-AI-010050.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-26T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-26T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-17-3",
    "review_id": "seed-review-17-3",
    "user_id": "seed-user-017",
    "product_id": "seed-prod-ai-001",
    "order_id": "10051",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 17, order ORD-AI-010051.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-27T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-27T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-18-1",
    "review_id": "seed-review-18-1",
    "user_id": "seed-user-018",
    "product_id": "seed-prod-ai-006",
    "order_id": "10052",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 18, order ORD-AI-010052.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-26T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-26T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-18-2",
    "review_id": "seed-review-18-2",
    "user_id": "seed-user-018",
    "product_id": "seed-prod-ai-001",
    "order_id": "10053",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 18, order ORD-AI-010053.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-27T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-27T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-18-3",
    "review_id": "seed-review-18-3",
    "user_id": "seed-user-018",
    "product_id": "seed-prod-ai-002",
    "order_id": "10054",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 18, order ORD-AI-010054.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-28T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-28T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-19-1",
    "review_id": "seed-review-19-1",
    "user_id": "seed-user-019",
    "product_id": "seed-prod-ai-001",
    "order_id": "10055",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 19, order ORD-AI-010055.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-27T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-27T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-19-2",
    "review_id": "seed-review-19-2",
    "user_id": "seed-user-019",
    "product_id": "seed-prod-ai-002",
    "order_id": "10056",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 19, order ORD-AI-010056.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-28T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-28T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-19-3",
    "review_id": "seed-review-19-3",
    "user_id": "seed-user-019",
    "product_id": "seed-prod-ai-003",
    "order_id": "10057",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 19, order ORD-AI-010057.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-29T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-29T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-20-1",
    "review_id": "seed-review-20-1",
    "user_id": "seed-user-020",
    "product_id": "seed-prod-ai-002",
    "order_id": "10058",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 20, order ORD-AI-010058.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-02-28T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-02-28T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-20-2",
    "review_id": "seed-review-20-2",
    "user_id": "seed-user-020",
    "product_id": "seed-prod-ai-003",
    "order_id": "10059",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 20, order ORD-AI-010059.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-29T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-29T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-20-3",
    "review_id": "seed-review-20-3",
    "user_id": "seed-user-020",
    "product_id": "seed-prod-ai-004",
    "order_id": "10060",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 20, order ORD-AI-010060.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-30T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-30T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-21-1",
    "review_id": "seed-review-21-1",
    "user_id": "seed-user-021",
    "product_id": "seed-prod-ai-003",
    "order_id": "10061",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 21, order ORD-AI-010061.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-01T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-01T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-21-2",
    "review_id": "seed-review-21-2",
    "user_id": "seed-user-021",
    "product_id": "seed-prod-ai-004",
    "order_id": "10062",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 21, order ORD-AI-010062.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-30T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-30T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-21-3",
    "review_id": "seed-review-21-3",
    "user_id": "seed-user-021",
    "product_id": "seed-prod-ai-005",
    "order_id": "10063",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 21, order ORD-AI-010063.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-01T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-01T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-22-1",
    "review_id": "seed-review-22-1",
    "user_id": "seed-user-022",
    "product_id": "seed-prod-ai-004",
    "order_id": "10064",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 22, order ORD-AI-010064.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-02T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-02T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-22-2",
    "review_id": "seed-review-22-2",
    "user_id": "seed-user-022",
    "product_id": "seed-prod-ai-005",
    "order_id": "10065",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 22, order ORD-AI-010065.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-31T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-31T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-22-3",
    "review_id": "seed-review-22-3",
    "user_id": "seed-user-022",
    "product_id": "seed-prod-ai-006",
    "order_id": "10066",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 22, order ORD-AI-010066.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-02T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-02T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-23-1",
    "review_id": "seed-review-23-1",
    "user_id": "seed-user-023",
    "product_id": "seed-prod-ai-005",
    "order_id": "10067",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 23, order ORD-AI-010067.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-03T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-03T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-23-2",
    "review_id": "seed-review-23-2",
    "user_id": "seed-user-023",
    "product_id": "seed-prod-ai-006",
    "order_id": "10068",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 23, order ORD-AI-010068.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-01T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-01T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-23-3",
    "review_id": "seed-review-23-3",
    "user_id": "seed-user-023",
    "product_id": "seed-prod-ai-001",
    "order_id": "10069",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 23, order ORD-AI-010069.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-03T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-03T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-24-1",
    "review_id": "seed-review-24-1",
    "user_id": "seed-user-024",
    "product_id": "seed-prod-ai-006",
    "order_id": "10070",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 24, order ORD-AI-010070.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-04T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-04T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-24-2",
    "review_id": "seed-review-24-2",
    "user_id": "seed-user-024",
    "product_id": "seed-prod-ai-001",
    "order_id": "10071",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 24, order ORD-AI-010071.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-02T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-02T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-24-3",
    "review_id": "seed-review-24-3",
    "user_id": "seed-user-024",
    "product_id": "seed-prod-ai-002",
    "order_id": "10072",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 24, order ORD-AI-010072.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-04T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-04T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-25-1",
    "review_id": "seed-review-25-1",
    "user_id": "seed-user-025",
    "product_id": "seed-prod-ai-001",
    "order_id": "10073",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 25, order ORD-AI-010073.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-05T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-05T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-25-2",
    "review_id": "seed-review-25-2",
    "user_id": "seed-user-025",
    "product_id": "seed-prod-ai-002",
    "order_id": "10074",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 25, order ORD-AI-010074.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-03T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-03T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-25-3",
    "review_id": "seed-review-25-3",
    "user_id": "seed-user-025",
    "product_id": "seed-prod-ai-003",
    "order_id": "10075",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 25, order ORD-AI-010075.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-05T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-05T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-26-1",
    "review_id": "seed-review-26-1",
    "user_id": "seed-user-026",
    "product_id": "seed-prod-ai-002",
    "order_id": "10076",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 26, order ORD-AI-010076.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-06T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-06T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-26-2",
    "review_id": "seed-review-26-2",
    "user_id": "seed-user-026",
    "product_id": "seed-prod-ai-003",
    "order_id": "10077",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 26, order ORD-AI-010077.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-04T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-04T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-26-3",
    "review_id": "seed-review-26-3",
    "user_id": "seed-user-026",
    "product_id": "seed-prod-ai-004",
    "order_id": "10078",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 26, order ORD-AI-010078.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-06T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-06T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-27-1",
    "review_id": "seed-review-27-1",
    "user_id": "seed-user-027",
    "product_id": "seed-prod-ai-003",
    "order_id": "10079",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 27, order ORD-AI-010079.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-07T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-07T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-27-2",
    "review_id": "seed-review-27-2",
    "user_id": "seed-user-027",
    "product_id": "seed-prod-ai-004",
    "order_id": "10080",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 27, order ORD-AI-010080.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-05T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-05T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-27-3",
    "review_id": "seed-review-27-3",
    "user_id": "seed-user-027",
    "product_id": "seed-prod-ai-005",
    "order_id": "10081",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 27, order ORD-AI-010081.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-07T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-07T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-28-1",
    "review_id": "seed-review-28-1",
    "user_id": "seed-user-028",
    "product_id": "seed-prod-ai-004",
    "order_id": "10082",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 28, order ORD-AI-010082.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-08T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-08T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-28-2",
    "review_id": "seed-review-28-2",
    "user_id": "seed-user-028",
    "product_id": "seed-prod-ai-005",
    "order_id": "10083",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 28, order ORD-AI-010083.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-06T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-06T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-28-3",
    "review_id": "seed-review-28-3",
    "user_id": "seed-user-028",
    "product_id": "seed-prod-ai-006",
    "order_id": "10084",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 28, order ORD-AI-010084.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-08T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-08T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-29-1",
    "review_id": "seed-review-29-1",
    "user_id": "seed-user-029",
    "product_id": "seed-prod-ai-005",
    "order_id": "10085",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 29, order ORD-AI-010085.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-09T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-09T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-29-2",
    "review_id": "seed-review-29-2",
    "user_id": "seed-user-029",
    "product_id": "seed-prod-ai-006",
    "order_id": "10086",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 29, order ORD-AI-010086.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-07T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-07T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-29-3",
    "review_id": "seed-review-29-3",
    "user_id": "seed-user-029",
    "product_id": "seed-prod-ai-001",
    "order_id": "10087",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 29, order ORD-AI-010087.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-09T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-09T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-30-1",
    "review_id": "seed-review-30-1",
    "user_id": "seed-user-030",
    "product_id": "seed-prod-ai-006",
    "order_id": "10088",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 30, order ORD-AI-010088.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-10T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-10T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-30-2",
    "review_id": "seed-review-30-2",
    "user_id": "seed-user-030",
    "product_id": "seed-prod-ai-001",
    "order_id": "10089",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 30, order ORD-AI-010089.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-08T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-08T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-30-3",
    "review_id": "seed-review-30-3",
    "user_id": "seed-user-030",
    "product_id": "seed-prod-ai-002",
    "order_id": "10090",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 30, order ORD-AI-010090.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-10T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-10T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-31-1",
    "review_id": "seed-review-31-1",
    "user_id": "seed-user-031",
    "product_id": "seed-prod-ai-001",
    "order_id": "10091",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 31, order ORD-AI-010091.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-11T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-11T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-31-2",
    "review_id": "seed-review-31-2",
    "user_id": "seed-user-031",
    "product_id": "seed-prod-ai-002",
    "order_id": "10092",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 31, order ORD-AI-010092.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-09T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-09T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-31-3",
    "review_id": "seed-review-31-3",
    "user_id": "seed-user-031",
    "product_id": "seed-prod-ai-003",
    "order_id": "10093",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 31, order ORD-AI-010093.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-11T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-11T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-32-1",
    "review_id": "seed-review-32-1",
    "user_id": "seed-user-032",
    "product_id": "seed-prod-ai-002",
    "order_id": "10094",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 32, order ORD-AI-010094.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-12T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-12T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-32-2",
    "review_id": "seed-review-32-2",
    "user_id": "seed-user-032",
    "product_id": "seed-prod-ai-003",
    "order_id": "10095",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 32, order ORD-AI-010095.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-10T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-10T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-32-3",
    "review_id": "seed-review-32-3",
    "user_id": "seed-user-032",
    "product_id": "seed-prod-ai-004",
    "order_id": "10096",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 32, order ORD-AI-010096.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-12T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-12T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-33-1",
    "review_id": "seed-review-33-1",
    "user_id": "seed-user-033",
    "product_id": "seed-prod-ai-003",
    "order_id": "10097",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 33, order ORD-AI-010097.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-13T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-13T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-33-2",
    "review_id": "seed-review-33-2",
    "user_id": "seed-user-033",
    "product_id": "seed-prod-ai-004",
    "order_id": "10098",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 33, order ORD-AI-010098.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-11T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-11T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-33-3",
    "review_id": "seed-review-33-3",
    "user_id": "seed-user-033",
    "product_id": "seed-prod-ai-005",
    "order_id": "10099",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 33, order ORD-AI-010099.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-13T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-13T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-34-1",
    "review_id": "seed-review-34-1",
    "user_id": "seed-user-034",
    "product_id": "seed-prod-ai-004",
    "order_id": "10100",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 34, order ORD-AI-010100.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-14T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-14T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-34-2",
    "review_id": "seed-review-34-2",
    "user_id": "seed-user-034",
    "product_id": "seed-prod-ai-005",
    "order_id": "10101",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 34, order ORD-AI-010101.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-12T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-12T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-34-3",
    "review_id": "seed-review-34-3",
    "user_id": "seed-user-034",
    "product_id": "seed-prod-ai-006",
    "order_id": "10102",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 34, order ORD-AI-010102.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-14T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-14T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-35-1",
    "review_id": "seed-review-35-1",
    "user_id": "seed-user-035",
    "product_id": "seed-prod-ai-005",
    "order_id": "10103",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 35, order ORD-AI-010103.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-15T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-15T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-35-2",
    "review_id": "seed-review-35-2",
    "user_id": "seed-user-035",
    "product_id": "seed-prod-ai-006",
    "order_id": "10104",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 35, order ORD-AI-010104.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-13T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-13T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-35-3",
    "review_id": "seed-review-35-3",
    "user_id": "seed-user-035",
    "product_id": "seed-prod-ai-001",
    "order_id": "10105",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 35, order ORD-AI-010105.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-15T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-15T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-36-1",
    "review_id": "seed-review-36-1",
    "user_id": "seed-user-036",
    "product_id": "seed-prod-ai-006",
    "order_id": "10106",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 36, order ORD-AI-010106.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-16T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-16T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-36-2",
    "review_id": "seed-review-36-2",
    "user_id": "seed-user-036",
    "product_id": "seed-prod-ai-001",
    "order_id": "10107",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 36, order ORD-AI-010107.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-14T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-14T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-36-3",
    "review_id": "seed-review-36-3",
    "user_id": "seed-user-036",
    "product_id": "seed-prod-ai-002",
    "order_id": "10108",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 36, order ORD-AI-010108.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-16T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-16T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-37-1",
    "review_id": "seed-review-37-1",
    "user_id": "seed-user-037",
    "product_id": "seed-prod-ai-001",
    "order_id": "10109",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 37, order ORD-AI-010109.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-17T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-17T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-37-2",
    "review_id": "seed-review-37-2",
    "user_id": "seed-user-037",
    "product_id": "seed-prod-ai-002",
    "order_id": "10110",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 37, order ORD-AI-010110.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-15T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-15T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-37-3",
    "review_id": "seed-review-37-3",
    "user_id": "seed-user-037",
    "product_id": "seed-prod-ai-003",
    "order_id": "10111",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 37, order ORD-AI-010111.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-17T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-17T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-38-1",
    "review_id": "seed-review-38-1",
    "user_id": "seed-user-038",
    "product_id": "seed-prod-ai-002",
    "order_id": "10112",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 38, order ORD-AI-010112.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-18T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-18T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-38-2",
    "review_id": "seed-review-38-2",
    "user_id": "seed-user-038",
    "product_id": "seed-prod-ai-003",
    "order_id": "10113",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 38, order ORD-AI-010113.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-16T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-16T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-38-3",
    "review_id": "seed-review-38-3",
    "user_id": "seed-user-038",
    "product_id": "seed-prod-ai-004",
    "order_id": "10114",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 38, order ORD-AI-010114.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-18T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-18T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-39-1",
    "review_id": "seed-review-39-1",
    "user_id": "seed-user-039",
    "product_id": "seed-prod-ai-003",
    "order_id": "10115",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 39, order ORD-AI-010115.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-19T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-19T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-39-2",
    "review_id": "seed-review-39-2",
    "user_id": "seed-user-039",
    "product_id": "seed-prod-ai-004",
    "order_id": "10116",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 39, order ORD-AI-010116.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-17T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-17T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-39-3",
    "review_id": "seed-review-39-3",
    "user_id": "seed-user-039",
    "product_id": "seed-prod-ai-005",
    "order_id": "10117",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 39, order ORD-AI-010117.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-19T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-19T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-40-1",
    "review_id": "seed-review-40-1",
    "user_id": "seed-user-040",
    "product_id": "seed-prod-ai-004",
    "order_id": "10118",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 40, order ORD-AI-010118.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-20T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-20T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-40-2",
    "review_id": "seed-review-40-2",
    "user_id": "seed-user-040",
    "product_id": "seed-prod-ai-005",
    "order_id": "10119",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 40, order ORD-AI-010119.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-18T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-18T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-40-3",
    "review_id": "seed-review-40-3",
    "user_id": "seed-user-040",
    "product_id": "seed-prod-ai-006",
    "order_id": "10120",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 40, order ORD-AI-010120.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-20T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-20T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-41-1",
    "review_id": "seed-review-41-1",
    "user_id": "seed-user-041",
    "product_id": "seed-prod-ai-005",
    "order_id": "10121",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 41, order ORD-AI-010121.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-21T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-21T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-41-2",
    "review_id": "seed-review-41-2",
    "user_id": "seed-user-041",
    "product_id": "seed-prod-ai-006",
    "order_id": "10122",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 41, order ORD-AI-010122.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-19T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-19T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-41-3",
    "review_id": "seed-review-41-3",
    "user_id": "seed-user-041",
    "product_id": "seed-prod-ai-001",
    "order_id": "10123",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 41, order ORD-AI-010123.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-21T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-21T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-42-1",
    "review_id": "seed-review-42-1",
    "user_id": "seed-user-042",
    "product_id": "seed-prod-ai-006",
    "order_id": "10124",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 42, order ORD-AI-010124.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-22T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-22T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-42-2",
    "review_id": "seed-review-42-2",
    "user_id": "seed-user-042",
    "product_id": "seed-prod-ai-001",
    "order_id": "10125",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 42, order ORD-AI-010125.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-20T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-20T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-42-3",
    "review_id": "seed-review-42-3",
    "user_id": "seed-user-042",
    "product_id": "seed-prod-ai-002",
    "order_id": "10126",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 42, order ORD-AI-010126.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-22T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-22T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-43-1",
    "review_id": "seed-review-43-1",
    "user_id": "seed-user-043",
    "product_id": "seed-prod-ai-001",
    "order_id": "10127",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 43, order ORD-AI-010127.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-23T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-23T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-43-2",
    "review_id": "seed-review-43-2",
    "user_id": "seed-user-043",
    "product_id": "seed-prod-ai-002",
    "order_id": "10128",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 43, order ORD-AI-010128.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-21T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-21T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-43-3",
    "review_id": "seed-review-43-3",
    "user_id": "seed-user-043",
    "product_id": "seed-prod-ai-003",
    "order_id": "10129",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 43, order ORD-AI-010129.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-23T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-23T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-44-1",
    "review_id": "seed-review-44-1",
    "user_id": "seed-user-044",
    "product_id": "seed-prod-ai-002",
    "order_id": "10130",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 44, order ORD-AI-010130.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-24T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-24T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-44-2",
    "review_id": "seed-review-44-2",
    "user_id": "seed-user-044",
    "product_id": "seed-prod-ai-003",
    "order_id": "10131",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 44, order ORD-AI-010131.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-22T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-22T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-44-3",
    "review_id": "seed-review-44-3",
    "user_id": "seed-user-044",
    "product_id": "seed-prod-ai-004",
    "order_id": "10132",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 44, order ORD-AI-010132.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-24T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-24T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-45-1",
    "review_id": "seed-review-45-1",
    "user_id": "seed-user-045",
    "product_id": "seed-prod-ai-003",
    "order_id": "10133",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 45, order ORD-AI-010133.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-25T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-25T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-45-2",
    "review_id": "seed-review-45-2",
    "user_id": "seed-user-045",
    "product_id": "seed-prod-ai-004",
    "order_id": "10134",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 45, order ORD-AI-010134.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-23T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-23T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-45-3",
    "review_id": "seed-review-45-3",
    "user_id": "seed-user-045",
    "product_id": "seed-prod-ai-005",
    "order_id": "10135",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 45, order ORD-AI-010135.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-25T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-25T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-46-1",
    "review_id": "seed-review-46-1",
    "user_id": "seed-user-046",
    "product_id": "seed-prod-ai-004",
    "order_id": "10136",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 46, order ORD-AI-010136.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-26T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-26T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-46-2",
    "review_id": "seed-review-46-2",
    "user_id": "seed-user-046",
    "product_id": "seed-prod-ai-005",
    "order_id": "10137",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 46, order ORD-AI-010137.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-24T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-24T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-46-3",
    "review_id": "seed-review-46-3",
    "user_id": "seed-user-046",
    "product_id": "seed-prod-ai-006",
    "order_id": "10138",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 46, order ORD-AI-010138.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-26T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-26T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-47-1",
    "review_id": "seed-review-47-1",
    "user_id": "seed-user-047",
    "product_id": "seed-prod-ai-005",
    "order_id": "10139",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 47, order ORD-AI-010139.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-27T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-27T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-47-2",
    "review_id": "seed-review-47-2",
    "user_id": "seed-user-047",
    "product_id": "seed-prod-ai-006",
    "order_id": "10140",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 47, order ORD-AI-010140.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-25T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-25T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-47-3",
    "review_id": "seed-review-47-3",
    "user_id": "seed-user-047",
    "product_id": "seed-prod-ai-001",
    "order_id": "10141",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 47, order ORD-AI-010141.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-27T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-27T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-48-1",
    "review_id": "seed-review-48-1",
    "user_id": "seed-user-048",
    "product_id": "seed-prod-ai-006",
    "order_id": "10142",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 48, order ORD-AI-010142.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-28T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-28T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-48-2",
    "review_id": "seed-review-48-2",
    "user_id": "seed-user-048",
    "product_id": "seed-prod-ai-001",
    "order_id": "10143",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 48, order ORD-AI-010143.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-26T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-26T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-48-3",
    "review_id": "seed-review-48-3",
    "user_id": "seed-user-048",
    "product_id": "seed-prod-ai-002",
    "order_id": "10144",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 48, order ORD-AI-010144.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-28T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-28T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-49-1",
    "review_id": "seed-review-49-1",
    "user_id": "seed-user-049",
    "product_id": "seed-prod-ai-001",
    "order_id": "10145",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 49, order ORD-AI-010145.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-29T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-29T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-49-2",
    "review_id": "seed-review-49-2",
    "user_id": "seed-user-049",
    "product_id": "seed-prod-ai-002",
    "order_id": "10146",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 49, order ORD-AI-010146.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-27T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-27T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-49-3",
    "review_id": "seed-review-49-3",
    "user_id": "seed-user-049",
    "product_id": "seed-prod-ai-003",
    "order_id": "10147",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 49, order ORD-AI-010147.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-29T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-29T13:20:00.000Z"
    }
  },
  {
    "_id": "seed-review-50-1",
    "review_id": "seed-review-50-1",
    "user_id": "seed-user-050",
    "product_id": "seed-prod-ai-002",
    "order_id": "10148",
    "star": 5,
    "title": "Rat hai long",
    "content": "Chat vai tot, form dep, giao hang nhanh. Du lieu seed de test AI agent. User Khach hang AI 50, order ORD-AI-010148.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-03-30T13:00:00.000Z"
    },
    "updated_at": {
      "$date": "2026-03-30T13:00:00.000Z"
    }
  },
  {
    "_id": "seed-review-50-2",
    "review_id": "seed-review-50-2",
    "user_id": "seed-user-050",
    "product_id": "seed-prod-ai-003",
    "order_id": "10149",
    "star": 4,
    "title": "Dang tien mua lai",
    "content": "San pham dung mo ta, dong goi on, phu hop de test dashboard va AI. User Khach hang AI 50, order ORD-AI-010149.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-04-28T13:10:00.000Z"
    },
    "updated_at": {
      "$date": "2026-04-28T13:10:00.000Z"
    }
  },
  {
    "_id": "seed-review-50-3",
    "review_id": "seed-review-50-3",
    "user_id": "seed-user-050",
    "product_id": "seed-prod-ai-004",
    "order_id": "10150",
    "star": 5,
    "title": "Se gioi thieu",
    "content": "Trai nghiem mua hang muot, san pham on dinh, co the dung de phan tich hanh vi. User Khach hang AI 50, order ORD-AI-010150.",
    "images": [],
    "is_visible": true,
    "created_at": {
      "$date": "2026-05-30T13:20:00.000Z"
    },
    "updated_at": {
      "$date": "2026-05-30T13:20:00.000Z"
    }
  }
];
dbRef.reviews.bulkWrite(
  docs.map((doc) => ({
    replaceOne: {
      filter: { review_id: doc.review_id },
      replacement: doc,
      upsert: true,
    },
  })),
);
print(`Inserted or updated ${docs.length} review documents.`);
