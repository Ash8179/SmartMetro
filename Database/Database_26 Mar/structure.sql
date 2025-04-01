-- MySQL dump 10.13  Distrib 5.7.24, for osx11.1 (x86_64)
--
-- Host: localhost    Database: new_schema
-- ------------------------------------------------------
-- Server version	9.2.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `Dijkstra`
--

DROP TABLE IF EXISTS `Dijkstra`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Dijkstra` (
  `id` int NOT NULL AUTO_INCREMENT,
  `from_station` int DEFAULT NULL,
  `from_station_cn` varchar(50) DEFAULT NULL,
  `from_station_en` varchar(255) DEFAULT NULL,
  `to_station` int DEFAULT NULL,
  `to_station_cn` varchar(50) DEFAULT NULL,
  `to_station_en` varchar(255) DEFAULT NULL,
  `line_id` int DEFAULT NULL,
  `travel_time` int DEFAULT NULL,
  `from_station_travel_group` int DEFAULT NULL,
  `to_station_travel_group` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1021 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `line_schedule`
--

DROP TABLE IF EXISTS `line_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `line_schedule` (
  `id` int NOT NULL AUTO_INCREMENT,
  `line` int NOT NULL,
  `stat_id` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `station_code` varchar(255) DEFAULT NULL,
  `first_time` time DEFAULT NULL,
  `first_time_desc` text,
  `last_time` time DEFAULT NULL,
  `last_time_desc` text,
  `direction` int DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `firstarrival_time` int DEFAULT NULL,
  `lastarrival_time` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1424 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sh_stat_info`
--

DROP TABLE IF EXISTS `sh_stat_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sh_stat_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `stat_id` int DEFAULT NULL,
  `name_cn` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `stat_id` (`stat_id`)
) ENGINE=InnoDB AUTO_INCREMENT=529 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `station_map`
--

DROP TABLE IF EXISTS `station_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `station_map` (
  `stat_id` int NOT NULL,
  `name_cn` varchar(255) DEFAULT NULL,
  `name_en` varchar(255) DEFAULT NULL,
  `longitude` decimal(12,8) DEFAULT NULL,
  `latitude` decimal(12,8) DEFAULT NULL,
  `travel_group` varchar(255) DEFAULT NULL,
  `line` int DEFAULT NULL,
  `all_stations` varchar(2000) DEFAULT NULL,
  `associated_lines` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`stat_id`),
  CONSTRAINT `station_map_ibfk_1` FOREIGN KEY (`stat_id`) REFERENCES `sh_stat_info` (`stat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stations`
--

DROP TABLE IF EXISTS `stations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stations` (
  `id` int NOT NULL AUTO_INCREMENT,
  `stat_id` int DEFAULT NULL,
  `name_cn` varchar(255) NOT NULL,
  `name_en` varchar(255) NOT NULL,
  `pinyin` varchar(50) DEFAULT NULL,
  `station_code` varchar(50) DEFAULT NULL,
  `line` varchar(50) DEFAULT NULL,
  `longitude` float DEFAULT NULL,
  `latitude` float DEFAULT NULL,
  `gao_lng` float DEFAULT NULL,
  `gao_lat` float DEFAULT NULL,
  `x` float DEFAULT NULL,
  `y` float DEFAULT NULL,
  `station_pic` varchar(255) DEFAULT NULL,
  `toilet_inside` tinyint(1) DEFAULT NULL,
  `toilet_position` text,
  `toilet_position_en` text,
  `entrance_info` text,
  `entrance_info_en` text,
  `street_pic` varchar(255) DEFAULT NULL,
  `elevator` text,
  `elevator_en` text,
  `terminal` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=522 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transfer_schedule`
--

DROP TABLE IF EXISTS `transfer_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `transfer_schedule` (
  `id` int NOT NULL AUTO_INCREMENT,
  `line` int NOT NULL,
  `stat_id` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `station_code` varchar(255) DEFAULT NULL,
  `first_time` time DEFAULT NULL,
  `first_time_desc` text,
  `last_time` time DEFAULT NULL,
  `last_time_desc` text,
  `direction` int DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `firstarrival_time` int DEFAULT NULL,
  `lastarrival_time` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1346 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `travel_info`
--

DROP TABLE IF EXISTS `travel_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `travel_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `stat_id` int DEFAULT NULL,
  `name_cn` varchar(255) NOT NULL,
  `travel_group` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=529 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-03-26 22:27:48
