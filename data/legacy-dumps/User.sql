-- MySQL dump 10.13  Distrib 8.0.19, for Win64 (x86_64)
--
-- Host: localhost    Database: fashion_user_db
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
-- Table structure for table `addresses`
--

DROP TABLE IF EXISTS `addresses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `addresses` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone_number` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `street` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ward` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `city` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_default` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_address_user` (`user_id`),
  CONSTRAINT `fk_address_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `addresses`
--

LOCK TABLES `addresses` WRITE;
/*!40000 ALTER TABLE `addresses` DISABLE KEYS */;
INSERT INTO `addresses` VALUES ('49fcae59-f289-4f31-83ed-cabec7899b32','c1292eef-ef3e-4678-8ba6-20caf84e5fef','Huy Chan','0905046373','123','Xã Hợp Tiến','Thành phố Hải Phòng',1),('8dd7e1f7-10ca-423b-905f-571b98c05192','c8e1d227-2ac5-432e-9549-dafe40967a36','Trần Huy 123','0905046373','1103/29','Xã Hồi Xuân','Tỉnh Thanh Hóa',1),('c39b5a81-3f58-4b4c-adb9-67401f413568','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','Tran Huy','0905046373','123','Xã Phú Lệ','Tỉnh Thanh Hóa',1);
/*!40000 ALTER TABLE `addresses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `email_verification_tokens`
--

DROP TABLE IF EXISTS `email_verification_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `email_verification_tokens` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token` (`token`),
  KEY `idx_email_verification_token` (`token`),
  KEY `idx_email_verification_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `email_verification_tokens`
--

LOCK TABLES `email_verification_tokens` WRITE;
/*!40000 ALTER TABLE `email_verification_tokens` DISABLE KEYS */;
INSERT INTO `email_verification_tokens` VALUES ('35dbbcca-0a76-43d9-b3a5-314f2777c0d5','ba5552dd-d45a-4234-848e-1107a1eda3eb','18ff900d-6532-4932-8c08-79004013e357','2026-04-18 08:56:05','2026-04-17 08:56:05'),('47ab4c05-b328-4fbc-8a9b-2b37ad3d3c7c','a862cbc8-99cc-4085-a57f-e319335627e3','a218780d-1fe5-4046-ac0a-456ef1d69197','2026-04-18 10:06:15','2026-04-17 10:06:15'),('d6ee6386-7235-4663-8131-0d3a4d6c0e82','917997bd-9adc-46d1-86bd-c1c527ea03c8','10da1b7a-f5f5-4579-8671-cdac2d7c4ea9','2026-04-20 14:50:57','2026-04-19 14:50:57'),('e9b22c97-c4cf-4ce4-aa60-465d6b62b16b','41027960-e231-454a-b66c-e050d2247ff2','797a7c33-53ab-4b39-b08e-7da02f4abfad','2026-04-18 16:44:51','2026-04-17 16:44:51');
/*!40000 ALTER TABLE `email_verification_tokens` ENABLE KEYS */;
UNLOCK TABLES;

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
INSERT INTO `flyway_schema_history` VALUES (1,'1','<< Flyway Baseline >>','BASELINE','<< Flyway Baseline >>',NULL,'root','2026-03-29 08:10:13',0,1),(2,'2','add email verification','SQL','V2__add_email_verification.sql',696234994,'root','2026-04-16 17:58:56',101,1),(3,'3','align users legacy columns','SQL','V3__align_users_legacy_columns.sql',1870337864,'root','2026-04-16 17:58:56',367,1),(4,'4','drop district from addresses','SQL','V4__drop_district_from_addresses.sql',747045901,'root','2026-04-27 12:57:07',282,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `password_reset_tokens`
--

DROP TABLE IF EXISTS `password_reset_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_tokens` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expires_at` datetime NOT NULL,
  `used` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `token` (`token`),
  KEY `idx_reset_token` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password_reset_tokens`
--

LOCK TABLES `password_reset_tokens` WRITE;
/*!40000 ALTER TABLE `password_reset_tokens` DISABLE KEYS */;
/*!40000 ALTER TABLE `password_reset_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `refresh_tokens`
--

DROP TABLE IF EXISTS `refresh_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_tokens` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `revoked` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `token` (`token`),
  KEY `fk_refresh_user` (`user_id`),
  KEY `idx_refresh_token` (`token`),
  CONSTRAINT `fk_refresh_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `refresh_tokens`
--

LOCK TABLES `refresh_tokens` WRITE;
/*!40000 ALTER TABLE `refresh_tokens` DISABLE KEYS */;
INSERT INTO `refresh_tokens` VALUES ('12400b76-1e97-4a8e-b581-b2163e1ddf20','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0NjA1NzlAZ21haWwuY29tIiwidXNlcklkIjoiODkxZDViNmUtN2MxYy00YTU2LTliMmYtZTE4MjI5ZTljN2NkIiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NDMwNTkwLCJleHAiOjE3NzcwMzUzOTB9.bDpf-mWBgDu4r2CHfR9WBjcn7KPLSfuB9zVzeEJgFB1RkcNLMbnMop8wHaeYMj4v','2026-04-24 12:56:31','2026-04-17 12:56:31',1),('13a6efca-0071-405a-8a55-e1dcc4ab8e1a','c8e1d227-2ac5-432e-9549-dafe40967a36','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0cmFuOTg0NDdAZ21haWwuY29tIiwidXNlcklkIjoiYzhlMWQyMjctMmFjNS00MzJlLTk1NDktZGFmZTQwOTY3YTM2Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjExMzM2LCJleHAiOjE3NzcyMTYxMzZ9.CJI0PBorglCW9V_-fbgHFdy8txzUVZBjsXgGOM_yYC3n35smuguK6M8gu-ICp-ym','2026-04-26 15:08:57','2026-04-19 15:08:57',0),('15d12d70-bd66-4a35-83ed-a8a798f91753','c8e1d227-2ac5-432e-9549-dafe40967a36','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0cmFuOTg0NDdAZ21haWwuY29tIiwidXNlcklkIjoiYzhlMWQyMjctMmFjNS00MzJlLTk1NDktZGFmZTQwOTY3YTM2Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc3NjUwNjUyLCJleHAiOjE3NzgyNTU0NTJ9._o0OkaM3DeGi-QAvL9rvtYkpZIhMM7rc5WiitO2D9HsyW8IbrAPPeccDQqRwbUZy','2026-05-08 15:50:52','2026-05-01 15:50:52',0),('1675e2ee-e940-461b-833d-f782f947d67c','917997bd-9adc-46d1-86bd-c1c527ea03c8','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0X2RlYnVnQGZhc2hpb24uY29tIiwidXNlcklkIjoiOTE3OTk3YmQtOWFkYy00NmQxLTg2YmQtYzFjNTI3ZWEwM2M4Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjEwMzYzLCJleHAiOjE3NzcyMTUxNjN9.6l01vDYm3FDzshDWyQ64sVIqxV5uayyFBzhJWlUwwbIDH3NIEeI6TrzY_TIJBzyk','2026-04-26 14:52:43','2026-04-19 14:52:43',0),('1f925cb3-99c1-4c2e-a3a9-5bdf1fbf3397','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0NjA1NzlAZ21haWwuY29tIiwidXNlcklkIjoiODkxZDViNmUtN2MxYy00YTU2LTliMmYtZTE4MjI5ZTljN2NkIiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc3MzAyNDI0LCJleHAiOjE3Nzc5MDcyMjR9.oeeLOwWSOagyDUZyrYYhUcdJhCxbI4xVOCwhaGH23pUar9hv6AjEEYq6_-41Hd8w','2026-05-04 15:07:04','2026-04-27 15:07:04',1),('3235eb0f-c4b1-4e5e-99d3-2080766a57eb','917997bd-9adc-46d1-86bd-c1c527ea03c8','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0X2RlYnVnQGZhc2hpb24uY29tIiwidXNlcklkIjoiOTE3OTk3YmQtOWFkYy00NmQxLTg2YmQtYzFjNTI3ZWEwM2M4Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjEwNDQxLCJleHAiOjE3NzcyMTUyNDF9.IwWHwZLyVoKp1-vvPORimjvYsBDV_sb9zg8-yvFX_5odTchAxnH8fq_7UKJ7xClS','2026-04-26 14:54:01','2026-04-19 14:54:01',0),('445c3de2-0c15-45b8-a649-ea8434b0e5f3','917997bd-9adc-46d1-86bd-c1c527ea03c8','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0X2RlYnVnQGZhc2hpb24uY29tIiwidXNlcklkIjoiOTE3OTk3YmQtOWFkYy00NmQxLTg2YmQtYzFjNTI3ZWEwM2M4Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjEwOTQwLCJleHAiOjE3NzcyMTU3NDB9.pRN97NnORf9pprTHMPL5bwsvxLhSY9OQimtQ0PRrupasHA9rB-S3TIjnHSygUPxD','2026-04-26 15:02:21','2026-04-19 15:02:21',0),('53f86320-b3d4-49fa-8b79-13d9af348c17','917997bd-9adc-46d1-86bd-c1c527ea03c8','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0X2RlYnVnQGZhc2hpb24uY29tIiwidXNlcklkIjoiOTE3OTk3YmQtOWFkYy00NmQxLTg2YmQtYzFjNTI3ZWEwM2M4Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjEwMzkzLCJleHAiOjE3NzcyMTUxOTN9.7hl6JwZDf-Z_aqDHvp6mBmmkLgNg5arU7lyfMQsNnhJtrjq4__yQSIvCnorelau6','2026-04-26 14:53:14','2026-04-19 14:53:14',0),('56367ebe-87f7-41dc-ab1d-ba5e4d4860cc','c8e1d227-2ac5-432e-9549-dafe40967a36','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0cmFuOTg0NDdAZ21haWwuY29tIiwidXNlcklkIjoiYzhlMWQyMjctMmFjNS00MzJlLTk1NDktZGFmZTQwOTY3YTM2Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NTk5MTkwLCJleHAiOjE3NzcyMDM5OTB9.NQJj7_-1yj-0dH18uG9skD5QCNFg2HFa_tiFjM1N0F4lJXKyjnVAk5Ub3bQWxLL2','2026-04-26 11:46:31','2026-04-19 11:46:31',0),('5e09bfe0-edb1-4c96-9326-9f733afc5042','c8e1d227-2ac5-432e-9549-dafe40967a36','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0cmFuOTg0NDdAZ21haWwuY29tIiwidXNlcklkIjoiYzhlMWQyMjctMmFjNS00MzJlLTk1NDktZGFmZTQwOTY3YTM2Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjA2OTE2LCJleHAiOjE3NzcyMTE3MTZ9.2GZ8Srywf0SMfkVGHFXxRrjJZe3cMXIEOcrUE3GJU3ubf32dHAfKRhKt_za-0Lwi','2026-04-26 13:55:17','2026-04-19 13:55:17',0),('6a65c694-b422-45b2-a50a-4b572c4d9c39','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0NjA1NzlAZ21haWwuY29tIiwidXNlcklkIjoiODkxZDViNmUtN2MxYy00YTU2LTliMmYtZTE4MjI5ZTljN2NkIiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc4MDQwMjEyLCJleHAiOjE3Nzg2NDUwMTJ9.vPekPMdA5NLVpmmdB-OFRDbX4np_iT12u2AhSaZVinTidbFnY06151L3nKt5jqFk','2026-05-13 04:03:32','2026-05-06 04:03:32',1),('70b8df54-8daf-43a2-b366-975c3e4b5a86','917997bd-9adc-46d1-86bd-c1c527ea03c8','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0X2RlYnVnQGZhc2hpb24uY29tIiwidXNlcklkIjoiOTE3OTk3YmQtOWFkYy00NmQxLTg2YmQtYzFjNTI3ZWEwM2M4Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjExMTcxLCJleHAiOjE3NzcyMTU5NzF9.XBD_8rUWrLORnekApw1s2CWSzFe4qWL-2rKjaiXVKseMHE5oD516f6bZwA6qBWO0','2026-04-26 15:06:11','2026-04-19 15:06:11',0),('83917d25-d125-4258-81c4-4ed7aec0e212','c1292eef-ef3e-4678-8ba6-20caf84e5fef','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0cmFuaHV5MTkxMTIwMDRAZ21haWwuY29tIiwidXNlcklkIjoiYzEyOTJlZWYtZWYzZS00Njc4LThiYTYtMjBjYWY4NGU1ZmVmIiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc4NTA4NzQ0LCJleHAiOjE3NzkxMTM1NDR9.5oo3xnAy3vIINhYOkO56mdo6Nqnw_6wV1Iw78rexmN4IWhF2V7Qppjeh6u0OFNi2','2026-05-18 14:12:24','2026-05-11 14:12:24',0),('8a17e091-5bc9-4daa-8acd-cdee545a331b','c8e1d227-2ac5-432e-9549-dafe40967a36','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0cmFuOTg0NDdAZ21haWwuY29tIiwidXNlcklkIjoiYzhlMWQyMjctMmFjNS00MzJlLTk1NDktZGFmZTQwOTY3YTM2Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjA3MzIzLCJleHAiOjE3NzcyMTIxMjN9.Tp8-CVLk_zBRhRzIW2iAHnkB3wt74MRDn-eqgDhTcHD9VORF7SM2ghTaVRCUDE-H','2026-04-26 14:02:04','2026-04-19 14:02:04',0),('9e08d55e-0b14-4f10-a9aa-a8deaef1bb95','917997bd-9adc-46d1-86bd-c1c527ea03c8','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0X2RlYnVnQGZhc2hpb24uY29tIiwidXNlcklkIjoiOTE3OTk3YmQtOWFkYy00NmQxLTg2YmQtYzFjNTI3ZWEwM2M4Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjEwMzg2LCJleHAiOjE3NzcyMTUxODZ9.ZL-r84CJDXzNrwaf1BtTzYiZILwvDMbvw8FwdF7OEzcnvdvmGBOxeGpcwXP7Z_bc','2026-04-26 14:53:06','2026-04-19 14:53:06',0),('a185343d-29d0-4d79-b6a8-afc5f210c31d','917997bd-9adc-46d1-86bd-c1c527ea03c8','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0X2RlYnVnQGZhc2hpb24uY29tIiwidXNlcklkIjoiOTE3OTk3YmQtOWFkYy00NmQxLTg2YmQtYzFjNTI3ZWEwM2M4Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjEwNzY2LCJleHAiOjE3NzcyMTU1NjZ9.v4WZ3aZ5KyuCfcjk-uniuiBFUI3eqzVDfcKexxC_T_2jF63FcOcNQBKNTVtduiTt','2026-04-26 14:59:27','2026-04-19 14:59:27',0),('a3116a3c-5828-4832-b2d8-57a858f4410d','917997bd-9adc-46d1-86bd-c1c527ea03c8','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0X2RlYnVnQGZhc2hpb24uY29tIiwidXNlcklkIjoiOTE3OTk3YmQtOWFkYy00NmQxLTg2YmQtYzFjNTI3ZWEwM2M4Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjEwMzc0LCJleHAiOjE3NzcyMTUxNzR9.Z8yts9pDmCjagSXnoyOHcpHDVEb7WpNnWAM6V2iHk5Yg0LwtWbP6hgnwwNGDjjiM','2026-04-26 14:52:55','2026-04-19 14:52:55',0),('a540a51a-e298-48fe-818b-8d8bd9a82e2b','c8e1d227-2ac5-432e-9549-dafe40967a36','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0cmFuOTg0NDdAZ21haWwuY29tIiwidXNlcklkIjoiYzhlMWQyMjctMmFjNS00MzJlLTk1NDktZGFmZTQwOTY3YTM2Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjExMzY2LCJleHAiOjE3NzcyMTYxNjZ9.avc9zWtZVPMlw_yHZ-mUZrNu7vSsBnxddnvuiMjE798biwFwcazdrP3IGqqZNm9f','2026-04-26 15:09:27','2026-04-19 15:09:27',0),('b1cc9282-db38-4bd3-ad9b-d3407edd55ca','c8e1d227-2ac5-432e-9549-dafe40967a36','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0cmFuOTg0NDdAZ21haWwuY29tIiwidXNlcklkIjoiYzhlMWQyMjctMmFjNS00MzJlLTk1NDktZGFmZTQwOTY3YTM2Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjA3MzM0LCJleHAiOjE3NzcyMTIxMzR9.U7i3bG2AZk1vn4--ZJ358u5jNvRcJC97oYdwfAlhenk6UM9R-P5tGQC1JkVpfZrb','2026-04-26 14:02:15','2026-04-19 14:02:15',0),('bcf7cb27-d2a1-4bc4-92ec-d444bf7240df','c8e1d227-2ac5-432e-9549-dafe40967a36','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0cmFuOTg0NDdAZ21haWwuY29tIiwidXNlcklkIjoiYzhlMWQyMjctMmFjNS00MzJlLTk1NDktZGFmZTQwOTY3YTM2Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NTk5OTE4LCJleHAiOjE3NzcyMDQ3MTh9.XE98tMyCyEF-nDRvJsK9TzSHmdfaZIsUXjFFNcWS3D7D4Yk_Mg-Q90bI7lCK5JYm','2026-04-26 11:58:39','2026-04-19 11:58:39',0),('bf1d4cd8-a34c-43e0-8b05-31f05daa42c2','c8e1d227-2ac5-432e-9549-dafe40967a36','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0cmFuOTg0NDdAZ21haWwuY29tIiwidXNlcklkIjoiYzhlMWQyMjctMmFjNS00MzJlLTk1NDktZGFmZTQwOTY3YTM2Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjA5OTk3LCJleHAiOjE3NzcyMTQ3OTd9.ae7sBO6CmVRfEigCoJVCA3i328b7fi8oKUN7dRCqUWqPQfucYl2l11sCFeqiubeu','2026-04-26 14:46:38','2026-04-19 14:46:38',0),('c45be7a8-7422-44df-a96b-b38e6c914893','917997bd-9adc-46d1-86bd-c1c527ea03c8','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0X2RlYnVnQGZhc2hpb24uY29tIiwidXNlcklkIjoiOTE3OTk3YmQtOWFkYy00NmQxLTg2YmQtYzFjNTI3ZWEwM2M4Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjEwMjc5LCJleHAiOjE3NzcyMTUwNzl9.CBbmx6g-pW8fwJoekL9Iljgp62QhqSw73SezztOx2STKqIZREn3x01Lf2YV_dfSh','2026-04-26 14:51:19','2026-04-19 14:51:19',0),('d4ec78d5-1f1c-4370-8267-0b015c7bc618','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0NjA1NzlAZ21haWwuY29tIiwidXNlcklkIjoiODkxZDViNmUtN2MxYy00YTU2LTliMmYtZTE4MjI5ZTljN2NkIiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc3OTEwNzMyLCJleHAiOjE3Nzg1MTU1MzJ9.QSwyjnn9uYKYHbSuh7L-UShIoBwfdFuLSXev7eCcCc0Sv9dTnLq4NK99Cmq5RDon','2026-05-11 16:05:33','2026-05-04 16:05:33',1),('e6b49cc1-34b8-4237-961d-cd1ad168ca96','c8e1d227-2ac5-432e-9549-dafe40967a36','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0cmFuOTg0NDdAZ21haWwuY29tIiwidXNlcklkIjoiYzhlMWQyMjctMmFjNS00MzJlLTk1NDktZGFmZTQwOTY3YTM2Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjA3MDM1LCJleHAiOjE3NzcyMTE4MzV9.biNpJfdjRHZuuLe5TllStQJdbrru_8NUX_V3xe0smVPtVKuJC_Rm8XO-TRKELAHf','2026-04-26 13:57:16','2026-04-19 13:57:16',0),('eaaf97ca-85f8-406f-a75c-b26013e41a68','917997bd-9adc-46d1-86bd-c1c527ea03c8','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0X2RlYnVnQGZhc2hpb24uY29tIiwidXNlcklkIjoiOTE3OTk3YmQtOWFkYy00NmQxLTg2YmQtYzFjNTI3ZWEwM2M4Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjEwNDAyLCJleHAiOjE3NzcyMTUyMDJ9.HiNRsyrQ8jRjC09gmlIdB5UEyEGJpFk-M3r-2taqTvqkmcX2BsRj5JCsG8kHoCrh','2026-04-26 14:53:23','2026-04-19 14:53:23',0),('f81dde47-ce45-4b9d-b267-a5a5460aac79','c8e1d227-2ac5-432e-9549-dafe40967a36','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0cmFuOTg0NDdAZ21haWwuY29tIiwidXNlcklkIjoiYzhlMWQyMjctMmFjNS00MzJlLTk1NDktZGFmZTQwOTY3YTM2Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NDQ0NTUyLCJleHAiOjE3NzcwNDkzNTJ9.LfJdDJhsjgu5Bc5R9oeEEchLrdpki5I3hVWDP0w8CyY3M9Zf6I3evqxl5c-Wl1Yd','2026-04-24 16:49:12','2026-04-17 16:49:12',0),('f8c7ff4f-0915-44aa-9183-8105e1887c98','891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJodXl0NjA1NzlAZ21haWwuY29tIiwidXNlcklkIjoiODkxZDViNmUtN2MxYy00YTU2LTliMmYtZTE4MjI5ZTljN2NkIiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc4NTA3NzkwLCJleHAiOjE3NzkxMTI1OTB9.iVBxAfzM7BGHFAj1F5avdJz-LAmTFE4OfPoK8528VKAHJ0FZ93pfXYTHNCYE6sYs','2026-05-18 13:56:30','2026-05-11 13:56:30',1),('faf2450f-9796-409d-8007-69afd5b03157','917997bd-9adc-46d1-86bd-c1c527ea03c8','eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0X2RlYnVnQGZhc2hpb24uY29tIiwidXNlcklkIjoiOTE3OTk3YmQtOWFkYy00NmQxLTg2YmQtYzFjNTI3ZWEwM2M4Iiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzc2NjEwNTU1LCJleHAiOjE3NzcyMTUzNTV9.83r95fG9cwpNqg7j4HpX6mcQ0nPIoDBDHp3_aiOtX0I6Rx6fGB_TNwDmJ2ZmXz15','2026-04-26 14:55:56','2026-04-19 14:55:56',0);
/*!40000 ALTER TABLE `refresh_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `first_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone_number` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` enum('CUSTOMER','ADMIN') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CUSTOMER',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `is_email_verified` tinyint(1) NOT NULL DEFAULT '0',
  `failed_login_attempts` int NOT NULL DEFAULT '0',
  `locked_until` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gender` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES ('41027960-e231-454a-b66c-e050d2247ff2','testuser678@gmail.com','$2a$10$.MY/tbc/Ysfm7DxaY3IEFuRJy7wyUJFoVuE5eWsfw/zTzTt6EDubO',NULL,NULL,NULL,NULL,'CUSTOMER',1,0,0,NULL,'2026-04-17 16:44:51','2026-04-17 16:44:51','Tester Manh','0987654321','/assets/images/avatarnam.png',0),('891d5b6e-7c1c-4a56-9b2f-e18229e9c7cd','huyt60579@gmail.com','$2a$10$siSF2n4D3vRusZ0hTZtlxuybnvkjimQs98vnkxYGbxtu/7kNzd6/G',NULL,NULL,NULL,NULL,'CUSTOMER',1,1,0,NULL,'2026-04-17 09:54:44','2026-04-17 12:50:41','Tran Huy','0905046373','/assets/images/avatarnam.png',0),('917997bd-9adc-46d1-86bd-c1c527ea03c8','test_debug@fashion.com','$2a$10$MBMxpNeyhB7Ig84Tv/m12.kb03Pi9lGitX0aNC5DXeY.88GZRO4Lu',NULL,NULL,NULL,NULL,'CUSTOMER',1,1,0,NULL,'2026-04-19 14:50:57','2026-04-19 14:50:57','Debug User','0901234567',NULL,NULL),('a862cbc8-99cc-4085-a57f-e319335627e3','tranhuy191104@gmail.com','$2a$10$Ji92bZ.fYZnsiytvtc6O3eTsUAaOPoGNiCOeDJ4CjWMIBc9XFh5VS',NULL,NULL,NULL,NULL,'CUSTOMER',1,0,0,NULL,'2026-04-17 10:06:15','2026-04-17 10:06:15','Trần Huy','0905036373','/assets/images/avatarnam.png',0),('c1292eef-ef3e-4678-8ba6-20caf84e5fef','tranhuy19112004@gmail.com','$2a$10$O.sjfk6i0c/gscOw8GxiXecy0.XwLjr8mKb9pqB45zUeBcdQr7snW',NULL,NULL,NULL,NULL,'CUSTOMER',1,1,0,NULL,'2026-05-11 14:11:43','2026-05-11 14:12:15','Huy Chan','0905046373','/assets/images/avatarnam.png',0),('c8e1d227-2ac5-432e-9549-dafe40967a36','huytran98447@gmail.com','$2a$10$TWQlo5x0BtEwIJvA1XazVOg1dRtcXryeLbvUMEwc630j0UvOD.a0K',NULL,NULL,NULL,NULL,'CUSTOMER',1,1,0,NULL,'2026-04-17 12:44:59','2026-04-17 12:45:28','Trần Huy','0905036373','/assets/images/avatarnam.png',0);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'fashion_user_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-13  0:44:58
