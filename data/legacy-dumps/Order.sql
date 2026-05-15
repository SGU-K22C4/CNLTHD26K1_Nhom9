-- MySQL dump 10.13  Distrib 8.0.19, for Win64 (x86_64)
--
-- Host: localhost    Database: fashion_order_db
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','init order','SQL','V1__init_order.sql',-993491701,'root','2026-03-27 08:27:59',73,1),(2,'2','alter product id varchar','SQL','V2__alter_product_id_varchar.sql',31901504,'root','2026-03-27 15:40:32',79,1),(3,'3','add loyalty fields','SQL','V3__add_loyalty_fields.sql',1962654856,'root','2026-04-16 18:17:39',47,1),(4,'4','add inventory reserved','SQL','V4__add_inventory_reserved.sql',-933486114,'root','2026-05-02 15:41:10',10,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `product_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_slug` varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `color` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `size` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `total_price` decimal(12,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_item_order` (`order_id`),
  CONSTRAINT `fk_item_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=74 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_items`
--

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;
INSERT INTO `order_items` VALUES (3,4,'04a26a6e-dc9c-4085-9328-1171bb3e22f1','ÁO VẢI RŨ TAY BỒNG','','https://static.zara.net/assets/public/cd5d/32be/3d014aa1b389/15c439ca8c1b/03666073403-p/03666073403-p.jpg?ts=1770811023290&w=500','Màu xanh da trời','L',2,1299000.00,2598000.00),(7,7,'04a26a6e-dc9c-4085-9328-1171bb3e22f1','ÁO VẢI RŨ TAY BỒNG','','https://static.zara.net/assets/public/cd5d/32be/3d014aa1b389/15c439ca8c1b/03666073403-p/03666073403-p.jpg?ts=1770811023290&w=500','Màu xanh da trời','L',2,1299000.00,2598000.00),(12,10,'04a26a6e-dc9c-4085-9328-1171bb3e22f1','ÁO VẢI RŨ',NULL,NULL,NULL,NULL,1,500000.00,500000.00),(13,11,'04a26a6e-dc9c-4085-9328-1171bb3e22f1','ÁO VẢI RŨ',NULL,NULL,NULL,NULL,3,500000.00,1500000.00),(14,12,'04a26a6e-dc9c-4085-9328-1171bb3e22f1','ÁO VẢI RŨ',NULL,NULL,NULL,NULL,3,500000.00,1500000.00),(15,13,'04a26a6e-dc9c-4085-9328-1171bb3e22f1','ÁO VẢI RŨ',NULL,NULL,NULL,NULL,3,500000.00,1500000.00),(26,24,'502265899','Good Product',NULL,NULL,NULL,NULL,1,100000.00,100000.00),(27,25,'502265899','Good Product',NULL,NULL,NULL,NULL,1,100000.00,100000.00),(28,26,'510420998','ĐẦM MIDI HAI DÂY KẺ CARO','','https://static.zara.net/assets/public/736e/9ac7/36c746ae98e4/67274deeb158/07969043300-a3/07969043300-a3.jpg?ts=1772718169234&w=500','Màu vàng','XL',3,1399000.00,4197000.00),(29,27,'509571903','CHÂN VÁY MIDI DỆT KIM MỊN','','https://static.zara.net/assets/public/2786/ba15/6b3147a19564/07a1ecedf1e5/02142286250-a3/02142286250-a3.jpg?ts=1772714342893&w=500','Màu trắng','L',6,1199000.00,7194000.00),(30,28,'495677799','E2E Product','e2e-product','','','44',1,1199000.00,1199000.00),(31,29,'495677799','E2E Product','e2e-product','','','44',1,1199000.00,1199000.00),(32,30,'495677799','E2E Product','e2e-product','','','44',1,1199000.00,1199000.00),(33,31,'495677799','E2E Product','e2e-product','','','44',1,1199000.00,1199000.00),(34,32,'495677799','E2E Product','e2e-product','','','44',1,1199000.00,1199000.00),(35,33,'495677799','E2E Product','e2e-product','','','44',1000,1199000.00,1199000000.00),(36,34,'02a6ad8f-6f1e-45cd-b4ca-beab32a99668','Saga Test Product','saga-test-product','','Kaki','XS',2,1199000.00,2398000.00),(37,35,'02a6ad8f-6f1e-45cd-b4ca-beab32a99668','Saga Test Product','saga-test-product','','Kaki','XS',60,1199000.00,71940000.00),(38,36,'02a6ad8f-6f1e-45cd-b4ca-beab32a99668','Saga Test Product','saga-test-product','','Kaki','XS',1,1199000.00,1199000.00),(39,37,'02a6ad8f-6f1e-45cd-b4ca-beab32a99668','Saga Test Product','saga-test-product','','Kaki','XS',1,1199000.00,1199000.00),(40,38,'02a6ad8f-6f1e-45cd-b4ca-beab32a99668','Saga Demo Product','saga-demo-product','','Kaki','XS',1,1199000.00,1199000.00),(41,39,'02a6ad8f-6f1e-45cd-b4ca-beab32a99668','Saga Demo Product','saga-demo-product','','Kaki','XS',50,1199000.00,59950000.00),(42,40,'02a6ad8f-6f1e-45cd-b4ca-beab32a99668','Saga Demo Product','saga-demo-product','','Kaki','XS',50,1199000.00,59950000.00),(43,41,'02a6ad8f-6f1e-45cd-b4ca-beab32a99668','Saga Demo Product','saga-demo-product','','Kaki','XS',50,1199000.00,59950000.00),(44,42,'02a6ad8f-6f1e-45cd-b4ca-beab32a99668','Saga Demo Product','saga-demo-product','','Kaki','XS',1,1199000.00,1199000.00),(45,43,'02a6ad8f-6f1e-45cd-b4ca-beab32a99668','Saga Demo Product','saga-demo-product','','Kaki','XS',1,1199000.00,1199000.00),(46,44,'02a6ad8f-6f1e-45cd-b4ca-beab32a99668','Saga Demo Product','saga-demo-product','','Kaki','XS',1,1199000.00,1199000.00),(47,45,'021ae64b-d3a0-4221-a59b-9c3e358a1dba','Quan Vay Gau Phoi Ren',NULL,NULL,'Black','M',1,1199000.00,1199000.00),(48,46,'021ae64b-d3a0-4221-a59b-9c3e358a1dba','Quan Vay Gau Phoi Ren',NULL,NULL,'Màu nâu','M',1,1199000.00,1199000.00),(49,47,'021ae64b-d3a0-4221-a59b-9c3e358a1dba','Quan Vay Gau Phoi Ren',NULL,NULL,'Brown','M',1,1199000.00,1199000.00),(50,48,'021ae64b-d3a0-4221-a59b-9c3e358a1dba','Quan Vay',NULL,NULL,'Kem','S',1,1199000.00,1199000.00),(51,49,'021ae64b-d3a0-4221-a59b-9c3e358a1dba','Quan Vay',NULL,NULL,'Màu nâu','M',1,1199000.00,1199000.00),(54,52,'021ae64b-d3a0-4221-a59b-9c3e358a1dba','Quan Vay',NULL,NULL,'Màu nâu','M',1,1199000.00,1199000.00),(55,53,'0267cd4e-04bd-4d96-a084-10f8ef61a98e','ÁO KHOÁC HIỆU ỨNG BẠC MÀU','','https://static.zara.net/assets/public/f13f/b163/37a14300a93c/e1336f030609/04341778707-p/04341778707-p.jpg?ts=1772715127484&w=500','Nâu vàng','XXL',2,1899000.00,3798000.00),(56,54,'0267cd4e-04bd-4d96-a084-10f8ef61a98e','ÁO KHOÁC HIỆU ỨNG BẠC MÀU','','https://static.zara.net/assets/public/f13f/b163/37a14300a93c/e1336f030609/04341778707-p/04341778707-p.jpg?ts=1772715127484&w=500','Nâu vàng','XXL',2,1899000.00,3798000.00),(57,55,'0267cd4e-04bd-4d96-a084-10f8ef61a98e','ÁO KHOÁC HIỆU ỨNG BẠC MÀU','','https://static.zara.net/assets/public/f13f/b163/37a14300a93c/e1336f030609/04341778707-p/04341778707-p.jpg?ts=1772715127484&w=500','Nâu vàng','XXL',2,1899000.00,3798000.00),(58,56,'0267cd4e-04bd-4d96-a084-10f8ef61a98e','ÁO KHOÁC HIỆU ỨNG BẠC MÀU','','https://static.zara.net/assets/public/f13f/b163/37a14300a93c/e1336f030609/04341778707-p/04341778707-p.jpg?ts=1772715127484&w=500','Nâu vàng','XXL',2,1899000.00,3798000.00),(59,57,'04a26a6e-dc9c-4085-9328-1171bb3e22f1','ÁO VẢI RŨ TAY BỒNG','','https://static.zara.net/assets/public/cd5d/32be/3d014aa1b389/15c439ca8c1b/03666073403-p/03666073403-p.jpg?ts=1770811023290&w=500','Màu xanh da trời','L',1,1299000.00,1299000.00),(60,57,'04a26a6e-dc9c-4085-9328-1171bb3e22f1','ÁO VẢI RŨ TAY BỒNG','','https://static.zara.net/assets/public/cd5d/32be/3d014aa1b389/15c439ca8c1b/03666073403-p/03666073403-p.jpg?ts=1770811023290&w=500','Màu xanh da trời','XL',1,1299000.00,1299000.00),(61,58,'04a26a6e-dc9c-4085-9328-1171bb3e22f1','ÁO VẢI RŨ TAY BỒNG','','https://static.zara.net/assets/public/cd5d/32be/3d014aa1b389/15c439ca8c1b/03666073403-p/03666073403-p.jpg?ts=1770811023290&w=500','Màu xanh da trời','S',1,1299000.00,1299000.00),(62,59,'0267cd4e-04bd-4d96-a084-10f8ef61a98e','ÁO KHOÁC HIỆU ỨNG BẠC MÀU','','https://static.zara.net/assets/public/f13f/b163/37a14300a93c/e1336f030609/04341778707-p/04341778707-p.jpg?ts=1772715127484&w=500','Nâu vàng','M',1,1899000.00,1899000.00),(63,60,'0267cd4e-04bd-4d96-a084-10f8ef61a98e','ÁO KHOÁC HIỆU ỨNG BẠC MÀU','','https://static.zara.net/assets/public/f13f/b163/37a14300a93c/e1336f030609/04341778707-p/04341778707-p.jpg?ts=1772715127484&w=500','Nâu vàng','XXL',2,1899000.00,3798000.00),(64,60,'04a26a6e-dc9c-4085-9328-1171bb3e22f1','ÁO VẢI RŨ TAY BỒNG','','https://static.zara.net/assets/public/cd5d/32be/3d014aa1b389/15c439ca8c1b/03666073403-p/03666073403-p.jpg?ts=1770811023290&w=500','Màu xanh da trời','L',1,1299000.00,1299000.00),(65,61,'0267cd4e-04bd-4d96-a084-10f8ef61a98e','ÁO KHOÁC HIỆU ỨNG BẠC MÀU','','https://static.zara.net/assets/public/f13f/b163/37a14300a93c/e1336f030609/04341778707-p/04341778707-p.jpg?ts=1772715127484&w=500','Nâu vàng','L',1,1899000.00,1899000.00),(66,62,'06f5e9ee-3489-483c-9aed-b68d8e3a942d','CHÂN VÁY NGẮN PHỒNG DÁNG CÓ CON ĐỈA','','https://static.zara.net/assets/public/963e/b1d1/68b54edf9539/8ee1e9c4f62f/04391413506-p/04391413506-p.jpg?ts=1769708171015&w=500','Màu vàng kaki nhạt','M',1,1199000.00,1199000.00),(67,63,'0267cd4e-04bd-4d96-a084-10f8ef61a98e','ÁO KHOÁC HIỆU ỨNG BẠC MÀU','','https://static.zara.net/assets/public/f13f/b163/37a14300a93c/e1336f030609/04341778707-p/04341778707-p.jpg?ts=1772715127484&w=500','Nâu vàng','L',2,1899000.00,3798000.00),(68,64,'0267cd4e-04bd-4d96-a084-10f8ef61a98e','ÁO KHOÁC HIỆU ỨNG BẠC MÀU','','https://static.zara.net/assets/public/f13f/b163/37a14300a93c/e1336f030609/04341778707-p/04341778707-p.jpg?ts=1772715127484&w=500','Nâu vàng','L',2,1899000.00,3798000.00),(69,65,'0267cd4e-04bd-4d96-a084-10f8ef61a98e','ÁO KHOÁC HIỆU ỨNG BẠC MÀU','','https://static.zara.net/assets/public/f13f/b163/37a14300a93c/e1336f030609/04341778707-p/04341778707-p.jpg?ts=1772715127484&w=500','Nâu vàng','L',2,1899000.00,3798000.00),(70,66,'0267cd4e-04bd-4d96-a084-10f8ef61a98e','ÁO KHOÁC HIỆU ỨNG BẠC MÀU','','https://static.zara.net/assets/public/f13f/b163/37a14300a93c/e1336f030609/04341778707-p/04341778707-p.jpg?ts=1772715127484&w=500','Nâu vàng','L',2,1899000.00,3798000.00),(71,67,'0267cd4e-04bd-4d96-a084-10f8ef61a98e','ÁO KHOÁC HIỆU ỨNG BẠC MÀU','','https://static.zara.net/assets/public/f13f/b163/37a14300a93c/e1336f030609/04341778707-p/04341778707-p.jpg?ts=1772715127484&w=500','Nâu vàng','L',2,1899000.00,3798000.00),(72,68,'06f5e9ee-3489-483c-9aed-b68d8e3a942d','CHÂN VÁY NGẮN PHỒNG DÁNG CÓ CON ĐỈA','','https://static.zara.net/assets/public/963e/b1d1/68b54edf9539/8ee1e9c4f62f/04391413506-p/04391413506-p.jpg?ts=1769708171015&w=500','Màu vàng kaki nhạt','M',1,1199000.00,1199000.00),(73,69,'1280b602-fce3-4669-90b0-4917512251e1','CHÂN VÁY MIDI XÒE','','https://static.zara.net/assets/public/82e1/be9d/11cd4d528b93/0c76ed0cea91/04387060250-p/04387060250-p.jpg?ts=1772796768282&w=500','Màu trắng','L',1,999000.00,999000.00);
/*!40000 ALTER TABLE `order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('PENDING','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED','RETURNED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `subtotal` decimal(12,2) NOT NULL,
  `shipping_fee` decimal(12,2) DEFAULT '0.00',
  `discount` decimal(12,2) DEFAULT '0.00',
  `loyalty_discount` decimal(12,2) DEFAULT '0.00',
  `used_points` int DEFAULT '0',
  `total` decimal(12,2) NOT NULL,
  `coupon_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `recipient_name` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `recipient_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shipping_address` text COLLATE utf8mb4_unicode_ci,
  `payment_method` enum('COD','BANK_TRANSFER','VNPAY','MOMO') COLLATE utf8mb4_unicode_ci DEFAULT 'COD',
  `payment_status` enum('PENDING','PAID','FAILED','REFUNDED') COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `note` text COLLATE utf8mb4_unicode_ci,
  `inventory_reserved` tinyint(1) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_number` (`order_number`),
  KEY `idx_order_user` (`user_id`),
  KEY `idx_order_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=70 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES (4,'ORD-1774627304303','guest-6b7a1b9b-b8fd-484d-86cc-10846f','PENDING',2598000.00,0.00,0.00,0.00,0,2598000.00,NULL,'á á','0905046373','sd, Phường Ninh Xá, Thị xã Thuận Thành, Tỉnh Bắc Ninh','COD','PENDING',NULL,NULL,'2026-03-27 16:01:44.326023','2026-03-27 16:01:44.326023'),(7,'ORD-1774628469227','guest-3cdd5587-1199-4df1-b4c2-04e517','PENDING',2598000.00,0.00,0.00,0.00,0,2598000.00,NULL,'sds dsdsd','0905046373','sdsd, Xã Hùng Việt, Huyện Cẩm Khê, Tỉnh Phú Thọ','COD','PENDING',NULL,NULL,'2026-03-27 16:21:09.229450','2026-03-27 16:21:09.229450'),(10,'ORD-1774633216387','guest-a483f3dd-7129-4485-8ee9-9c5d27','PENDING',500000.00,0.00,0.00,0.00,0,500000.00,NULL,'Test VNPay','0905046373','Hanoi, Vietnam','VNPAY','PENDING',NULL,NULL,'2026-03-27 17:40:16.402464','2026-03-27 17:40:16.402984'),(11,'ORD-1774633475380','guest-d1253acf-f5df-4e4b-92ea-120214','PENDING',1500000.00,0.00,0.00,0.00,0,1500000.00,NULL,'Test VNPay','0905046373','Hanoi, Vietnam','VNPAY','PENDING',NULL,NULL,'2026-03-27 17:44:35.391101','2026-03-27 17:44:35.391101'),(12,'ORD-1774633541446','guest-348d45c2-7699-4d2e-b3a4-ecd0d4','PENDING',1500000.00,0.00,0.00,0.00,0,1500000.00,NULL,'Test VNPay','0905046373','Hanoi, Vietnam','VNPAY','PENDING',NULL,NULL,'2026-03-27 17:45:41.448998','2026-03-27 17:45:41.449007'),(13,'ORD-1774633847202','guest-c8c32b21-ede5-40ed-995c-cd5f9d','CONFIRMED',1500000.00,0.00,0.00,0.00,0,1500000.00,NULL,'Test VNPay','0905046373','Hanoi, Vietnam','VNPAY','PAID',NULL,NULL,'2026-03-27 17:50:47.225350','2026-03-27 17:51:48.212524'),(24,'ORD-1774773363200','clean-check-user','CONFIRMED',100000.00,30000.00,0.00,0.00,0,130000.00,NULL,'Clean User','0900002222','789 Clean Street','COD','PAID',NULL,NULL,'2026-03-29 08:36:03.201037','2026-03-29 08:36:03.201057'),(25,'ORD-1774773445671','clean-check-user-2','CONFIRMED',100000.00,30000.00,0.00,0.00,0,130000.00,NULL,'Clean User','0900002222','789 Clean Street','COD','PAID',NULL,NULL,'2026-03-29 08:37:25.700632','2026-03-29 08:37:25.700694'),(26,'ORD-1774784778593','guest-ffd1ebef-915f-4f0c-b71c-b717c5','CONFIRMED',4197000.00,0.00,0.00,0.00,0,4197000.00,NULL,'á á','0905046373','sdsd, Xã Tuấn Đạo, Huyện Sơn Động, Tỉnh Bắc Giang','VNPAY','PAID',NULL,NULL,'2026-03-29 11:46:18.626015','2026-03-29 11:46:57.906015'),(27,'ORD-1775061881055','guest-d122358b-f6d4-4bb7-86dc-8fbd21','CONFIRMED',7194000.00,0.00,0.00,0.00,0,7194000.00,NULL,'Trần  Huy','0905046373','1103, Xã Hương Lung, Huyện Cẩm Khê, Tỉnh Phú Thọ','VNPAY','PAID',NULL,NULL,'2026-04-01 16:44:41.085569','2026-04-01 16:45:12.492284'),(28,'ORD-1775633224849','guest-7903b729-5fa8-41f2-8eab-7c01eb','CONFIRMED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'E2E Test User','0900000000','123 Test Street','VNPAY','PAID','e2e saga test',NULL,'2026-04-08 07:27:04.893329','2026-04-08 07:27:05.643362'),(29,'ORD-1775633255171','guest-c36ca974-2d91-4c79-bd8a-6f7efe','CONFIRMED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'E2E Test User','0900000000','123 Test Street','VNPAY','PAID','e2e saga test',NULL,'2026-04-08 07:27:35.173831','2026-04-08 07:27:35.288577'),(30,'ORD-1775633282543','guest-93748294-7c40-48d2-bb19-782b3b','CONFIRMED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'E2E Test User','0900000000','123 Test Street','VNPAY','PAID','e2e saga test',NULL,'2026-04-08 07:28:02.545198','2026-04-08 07:28:02.671207'),(31,'ORD-1775634077194','guest-589933a0-4168-466a-b415-7d70f7','CONFIRMED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'E2E Test User','0900000000','123 Test Street','VNPAY','PAID','e2e saga test',NULL,'2026-04-08 07:41:17.244523','2026-04-08 07:41:24.134906'),(32,'ORD-1775634642245','guest-72c85ccb-c0f9-4429-8246-548228','CONFIRMED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'E2E Test User','0900000000','123 Test Street','VNPAY','PAID','e2e saga test',NULL,'2026-04-08 07:50:42.250532','2026-04-08 07:50:42.640846'),(33,'ORD-1775634870512','guest-2c853ebb-6728-4e96-9adc-e18482','CANCELLED',1199000000.00,0.00,0.00,0.00,0,1199000000.00,NULL,'E2E Fail User','0900000001','123 Fail Street','VNPAY','FAILED','e2e saga fail test',NULL,'2026-04-08 07:54:30.513756','2026-04-08 07:54:30.537977'),(34,'ORD-1775756514879','saga-e2e-user','CONFIRMED',2398000.00,0.00,0.00,0.00,0,2398000.00,NULL,'Saga E2E','0900000000','HCM','COD','PAID','auto test',NULL,'2026-04-09 17:41:54.919021','2026-04-09 17:41:55.503332'),(35,'ORD-1775756515076','saga-e2e-user','CANCELLED',71940000.00,0.00,0.00,0.00,0,71940000.00,NULL,'Saga E2E','0900000000','HCM','COD','FAILED','auto test',NULL,'2026-04-09 17:41:55.078083','2026-04-09 17:41:55.565089'),(36,'ORD-1775756515093','saga-e2e-user','CONFIRMED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'Saga E2E','0900000000','HCM','VNPAY','PAID','auto test',NULL,'2026-04-09 17:41:55.095158','2026-04-09 17:41:56.638091'),(37,'ORD-1775756515112','saga-e2e-user','CANCELLED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'Saga E2E','0900000000','HCM','VNPAY','FAILED','auto test',NULL,'2026-04-09 17:41:55.114223','2026-04-09 17:41:57.873321'),(38,'ORD-1775757283200','postman-saga-user','CONFIRMED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'Postman Saga','0900000000','HCM','COD','PAID','inventory success path',NULL,'2026-04-09 17:54:43.205402','2026-04-09 17:54:43.251741'),(39,'ORD-1775757438672','postman-saga-user','CANCELLED',59950000.00,0.00,0.00,0.00,0,59950000.00,NULL,'Postman Saga','0900000000','HCM','COD','FAILED','inventory failure path',NULL,'2026-04-09 17:57:18.675886','2026-04-09 17:57:18.705585'),(40,'ORD-1775757533687','postman-saga-user','CANCELLED',59950000.00,0.00,0.00,0.00,0,59950000.00,NULL,'Postman Saga','0900000000','HCM','COD','FAILED','inventory failure path',NULL,'2026-04-09 17:58:53.692250','2026-04-09 17:58:53.725437'),(41,'ORD-1775757998080','postman-saga-user','CANCELLED',59950000.00,0.00,0.00,0.00,0,59950000.00,NULL,'Postman Saga','0900000000','HCM','COD','FAILED','inventory failure path',NULL,'2026-04-09 18:06:38.086318','2026-04-09 18:06:38.142806'),(42,'ORD-1775758006316','postman-saga-user','PENDING',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'Postman Saga','0900000000','HCM','VNPAY','PENDING','vnpay success path',NULL,'2026-04-09 18:06:46.318090','2026-04-09 18:06:46.318119'),(43,'ORD-1775758055513','postman-saga-user','CANCELLED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'Postman Saga','0900000000','HCM','VNPAY','FAILED','vnpay fail path',NULL,'2026-04-09 18:07:35.517077','2026-04-09 18:08:31.085199'),(44,'ORD-1775758703964','postman-saga-user','CONFIRMED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'Postman Saga','0900000000','HCM','COD','PAID','inventory success path',NULL,'2026-04-09 18:18:23.970006','2026-04-09 18:18:24.019615'),(45,'ORD-1776499096823-4502b7','test-audit-user','CANCELLED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'Nguyen Van A','0912345678','123 Le Loi, Quan 1, TP HCM','COD','FAILED','Test audit order',NULL,'2026-04-18 07:58:16.920636','2026-04-18 07:58:17.365118'),(46,'ORD-1776499143111-97aef8','test-audit-user','CONFIRMED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'Nguyen Van B','0987654321','456 Nguyen Hue, Quan 1, TP HCM','COD','PAID','Test order with real variant',NULL,'2026-04-18 07:59:03.141422','2026-04-18 07:59:03.167437'),(47,'ORD-1776499343909-591dd8','test-cancel-user','CANCELLED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'Test Cancel','0912345678','789 Test Street','VNPAY','FAILED',NULL,NULL,'2026-04-18 08:02:23.933631','2026-04-18 08:02:23.954261'),(48,'ORD-1776499375117-fc86d6','test-vnpay-user','CANCELLED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'Test VNPay','0912345678','789 Test VNPay Street','VNPAY','FAILED',NULL,NULL,'2026-04-18 08:02:55.141844','2026-04-18 08:02:55.159507'),(49,'ORD-1776499439886-5602c9','final-test-user','CONFIRMED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'Final Test','0912345678','123 Test St','COD','PAID',NULL,NULL,'2026-04-18 08:03:59.902462','2026-04-18 08:03:59.923884'),(52,'ORD-1776499629916-42279b','final-test-user','CONFIRMED',1199000.00,0.00,0.00,5000.00,50,1194000.00,NULL,'Final Test','0912345678','123 Test St','COD','PAID',NULL,NULL,'2026-04-18 08:07:09.939448','2026-04-18 08:07:09.984090'),(53,'ORD-1777347893612-d9394c','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','CANCELLED',3798000.00,0.00,0.00,0.00,0,3798000.00,NULL,'Tran Huy s','0905046373','123, Xã Phú Lệ, Tỉnh Thanh Hóa','VNPAY','FAILED',NULL,NULL,'2026-04-28 03:44:53.831713','2026-04-28 03:44:54.278303'),(54,'ORD-1777350604141-47fbf2','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','CANCELLED',3798000.00,0.00,0.00,0.00,0,3798000.00,NULL,'Tran Huy s','0905046373','123, Xã Phú Lệ, Tỉnh Thanh Hóa','VNPAY','FAILED',NULL,NULL,'2026-04-28 04:30:04.277876','2026-04-28 04:30:04.493579'),(55,'ORD-1777350687475-86f356','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','CANCELLED',3798000.00,0.00,0.00,0.00,0,3798000.00,NULL,'Tran Huy 123','0905046373','123, Xã Phú Lệ, Tỉnh Thanh Hóa','VNPAY','FAILED',NULL,NULL,'2026-04-28 04:31:27.488235','2026-04-28 04:31:27.508808'),(56,'ORD-1777351505086-d119c0','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','CANCELLED',3798000.00,0.00,0.00,0.00,0,3798000.00,NULL,'Tran Huy Huy','0905046373','123, Xã Phú Lệ, Tỉnh Thanh Hóa','VNPAY','FAILED',NULL,NULL,'2026-04-28 04:45:05.221410','2026-04-28 04:45:05.441790'),(57,'ORD-1777650709668-0d78c2','c8e1d227-2ac5-432e-9549-dafe40967a36','CANCELLED',2598000.00,0.00,0.00,0.00,0,2598000.00,NULL,'Trần Huy s','0905036373','1103/29, Xã Hồi Xuân, Tỉnh Thanh Hóa','COD','FAILED',NULL,NULL,'2026-05-01 15:51:49.816898','2026-05-01 15:51:50.371327'),(58,'ORD-1777651915034-b26077','c8e1d227-2ac5-432e-9549-dafe40967a36','CONFIRMED',1299000.00,0.00,0.00,0.00,0,1299000.00,NULL,'Trần Huy s','0905036373','1103/29, Xã Hồi Xuân, Tỉnh Thanh Hóa','VNPAY','PAID',NULL,NULL,'2026-05-01 16:11:55.112505','2026-05-01 16:12:45.701656'),(59,'ORD-1777735011006-adcd4c','c8e1d227-2ac5-432e-9549-dafe40967a36','CANCELLED',1899000.00,0.00,0.00,0.00,0,1899000.00,NULL,'Trần Huy s','0905036373','1103/29, Xã Hồi Xuân, Tỉnh Thanh Hóa','VNPAY','FAILED',NULL,NULL,'2026-05-02 15:16:51.140102','2026-05-02 15:16:51.590661'),(60,'ORD-1777735835521-b99f10','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','CANCELLED',5097000.00,0.00,0.00,0.00,0,5097000.00,NULL,'Tran Huy 123','0905046373','123, Xã Phú Lệ, Tỉnh Thanh Hóa','VNPAY','FAILED',NULL,NULL,'2026-05-02 15:30:35.663931','2026-05-02 15:30:36.083730'),(61,'ORD-1777736678082-26f4f2','c8e1d227-2ac5-432e-9549-dafe40967a36','CANCELLED',1899000.00,0.00,0.00,0.00,0,1899000.00,NULL,'Trần Huy abc','0905036373','1103/29, Xã Hồi Xuân, Tỉnh Thanh Hóa','VNPAY','FAILED',NULL,0,'2026-05-02 15:44:38.251036','2026-05-02 15:44:38.798239'),(62,'ORD-1777737122743-374c45','c8e1d227-2ac5-432e-9549-dafe40967a36','CONFIRMED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'Trần Huy ád','0905036373','1103/29, Xã Hồi Xuân, Tỉnh Thanh Hóa','VNPAY','PAID',NULL,1,'2026-05-02 15:52:02.754902','2026-05-02 15:52:35.500403'),(63,'ORD-1777910759186-2e2df5','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','PENDING',3798000.00,0.00,0.00,0.00,0,3798000.00,NULL,'Tran Huy','0905046373','123, Xã Phú Lệ, Tỉnh Thanh Hóa','VNPAY','PENDING',NULL,1,'2026-05-04 16:05:59.337221','2026-05-04 16:05:59.860141'),(64,'ORD-1777911227390-d7d23f','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','PENDING',3798000.00,0.00,0.00,0.00,0,3798000.00,NULL,'Tran Huy','0905046373','123, Xã Phú Lệ, Tỉnh Thanh Hóa','VNPAY','PENDING',NULL,1,'2026-05-04 16:13:47.502763','2026-05-04 16:13:47.689366'),(65,'ORD-1777911661987-d72dae','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','PENDING',3798000.00,0.00,0.00,0.00,0,3798000.00,NULL,'Tran Huy','0905046373','123, Xã Phú Lệ, Tỉnh Thanh Hóa','VNPAY','PENDING',NULL,1,'2026-05-04 16:21:02.118063','2026-05-04 16:21:02.321969'),(66,'ORD-1777911724251-b5c881','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','PENDING',3798000.00,0.00,0.00,0.00,0,3798000.00,NULL,'Tran Huy','0905046373','123, Xã Phú Lệ, Tỉnh Thanh Hóa','VNPAY','PENDING',NULL,1,'2026-05-04 16:22:04.262890','2026-05-04 16:22:04.283591'),(67,'ORD-1777912319332-2950c6','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','CONFIRMED',3798000.00,0.00,0.00,0.00,0,3798000.00,NULL,'Tran Huy','0905046373','123, Xã Phú Lệ, Tỉnh Thanh Hóa','VNPAY','PAID',NULL,1,'2026-05-04 16:31:59.345012','2026-05-04 16:32:31.647343'),(68,'ORD-1778605872297-9fbb9f','c1292eef-ef3e-4678-8ba6-20caf84e5fef','CONFIRMED',1199000.00,0.00,0.00,0.00,0,1199000.00,NULL,'Huy Chan','0905046373','123, Xã Hợp Tiến, Thành phố Hải Phòng','COD','PAID',NULL,1,'2026-05-12 17:11:12.365154','2026-05-12 17:26:54.182888'),(69,'ORD-1778606416455-809226','c1292eef-ef3e-4678-8ba6-20caf84e5fef','CONFIRMED',999000.00,0.00,0.00,0.00,0,999000.00,NULL,'Huy Chan','0905046373','123, Xã Hợp Tiến, Thành phố Hải Phòng','COD','PAID',NULL,1,'2026-05-12 17:20:16.467607','2026-05-12 17:35:54.147333');
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'fashion_order_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-13  0:44:13
