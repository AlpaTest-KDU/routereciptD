/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19-11.4.9-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: routereciptdb
-- ------------------------------------------------------
-- Server version	11.4.9-MariaDB-ubu2404
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Current Database: `routereciptdb`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `routereciptdb` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci */;

USE `routereciptdb`;

--
-- Table structure for table `notice`
--

DROP TABLE IF EXISTS `notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `notice` (
  `n_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `n_title` varchar(255) NOT NULL,
  `n_writer` varchar(50) NOT NULL,
  `n_content` varchar(255) NOT NULL,
  `n_create` date NOT NULL,
  PRIMARY KEY (`n_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notice`
--

LOCK TABLES `notice` WRITE;
/*!40000 ALTER TABLE `notice` DISABLE KEYS */;
/*!40000 ALTER TABLE `notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recipt`
--

DROP TABLE IF EXISTS `recipt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `recipt` (
  `r_no` bigint(20) NOT NULL AUTO_INCREMENT,
  `r_u` varchar(50) NOT NULL,
  `r_place` varchar(100) NOT NULL,
  `r_price` varchar(200) NOT NULL,
  `r_date` date NOT NULL,
  `r_goods` varchar(100) NOT NULL,
  `category` varchar(100) NOT NULL,
  `gender` enum('MALE','FEMALE') NOT NULL,
  PRIMARY KEY (`r_no`),
  KEY `r_u` (`r_u`),
  CONSTRAINT `recipt_ibfk_1` FOREIGN KEY (`r_u`) REFERENCES `user` (`u_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recipt`
--

LOCK TABLES `recipt` WRITE;
/*!40000 ALTER TABLE `recipt` DISABLE KEYS */;
/*!40000 ALTER TABLE `recipt` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `role_id` varchar(20) NOT NULL,
  `role_name` varchar(50) NOT NULL,
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `u_id` varchar(50) NOT NULL,
  `u_pw` varchar(100) NOT NULL,
  `u_name` varchar(50) NOT NULL,
  `u_birthday` date NOT NULL,
  `u_email` varchar(100) NOT NULL,
  `u_date` date NOT NULL,
  `gender` enum('MALE','FEMALE') NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'ROLE_USER',
  PRIMARY KEY (`u_id`),
  KEY `role` (`role`),
  CONSTRAINT `user_ibfk_1` FOREIGN KEY (`role`) REFERENCES `role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES
('admin','$2a$10$OykP3meELXjVZmrDldwVpuVBxqDJUTcAUZ6w8RMDHkz/hqboUk5tu','???','2025-12-09','admin@admin','2025-12-10','MALE','ROLE_ADMIN'),
('dhmkjeil','$2a$10$ZKuQLKaH5er47UsDOF7GB.kIW8v2l8Cp8RrUy2z/pKzjHZr/8LZNW','???','1994-09-29','dhmkjeil@naver.com','2025-12-15','MALE','ROLE_USER'),
('test','$2a$10$MiM0HNxT3xm5zTTTrZh5AuwB3ZJfSkK7BUOCrgoWRpN80YdE4cBEa','test','2025-12-15','test@test.com@','2025-12-12','MALE','ROLE_USER'),
('test2','$2a$10$tpiPmluys4Isz5Wp9qiqIeOp6zZ9vnVfPbHfU2exECy6H/LqmWFFy','test2','1999-09-09','test2@naver.com','2025-12-15','MALE','ROLE_USER'),
('test3','$2a$10$JXLeEyeHmy2mU1Q0/JSMeekPRVFVVmVKLW27i0yyQuiaCEjhCDQui','test3','2000-03-03','test3@naver.com','2025-12-15','FEMALE','ROLE_USER'),
('test4','$2a$10$4ReB/HnMuhnGItuO8aXLXuSJtpTv.Zgz0eZ8w8MydqEsNg2pQteuu','test4','2025-12-04','test4@test4@naver.com','2025-12-15','FEMALE','ROLE_USER');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'routereciptdb'
--

--
-- Dumping routines for database 'routereciptdb'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2025-12-15  5:19:44
