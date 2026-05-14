-- MySQL dump 10.13  Distrib 8.2.0, for Win64 (x86_64)
--
-- Host: localhost    Database: teacher_ethics
-- ------------------------------------------------------
-- Server version	8.2.0

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
-- Current Database: `teacher_ethics`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `teacher_ethics` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `teacher_ethics`;

--
-- Table structure for table `admins`
--

DROP TABLE IF EXISTS `admins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admins` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` varchar(50) NOT NULL COMMENT '管理员账号',
  `password` varchar(100) NOT NULL COMMENT '管理员密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '管理员真实姓名',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '账号状态：0禁用，1启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理端用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `answer_records`
--

DROP TABLE IF EXISTS `answer_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `answer_records` (
  `id` int NOT NULL AUTO_INCREMENT,
  `questionnaire_id` int DEFAULT NULL,
  `question_id` int DEFAULT NULL,
  `user_answer` json DEFAULT NULL,
  `user_score` int DEFAULT '0' COMMENT '本题得分',
  `is_marked` tinyint DEFAULT '0' COMMENT '是否被标记(0否，1是)',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `questionnaire_id` (`questionnaire_id`),
  KEY `question_id` (`question_id`),
  CONSTRAINT `answer_records_ibfk_1` FOREIGN KEY (`questionnaire_id`) REFERENCES `questionnaires` (`id`),
  CONSTRAINT `answer_records_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assessments`
--

DROP TABLE IF EXISTS `assessments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assessments` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '教师ID',
  `record_date` date NOT NULL COMMENT '评估日期',
  `overall_score` int NOT NULL COMMENT '综合得分（0-100）',
  `overall_level` tinyint NOT NULL COMMENT '综合等级：0优秀 1良好 2合格 3需改进',
  `learning_score` int NOT NULL COMMENT '学习筑基模块得分',
  `ability_score` int NOT NULL COMMENT '能力提升模块得分',
  `governance_score` int NOT NULL COMMENT '治理研修模块得分',
  `monthly_change` int DEFAULT NULL COMMENT '较上期变化值（可正可负）',
  `last_days` int DEFAULT NULL COMMENT '距离上次评估的天数',
  `total_activities` int DEFAULT NULL COMMENT '总活动数',
  `improvement_rate` int DEFAULT NULL COMMENT '提升率（百分比）',
  `ranking` int DEFAULT NULL COMMENT '排名百分位',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_record_date` (`user_id`,`record_date`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_record_date` (`record_date`),
  CONSTRAINT `fk_assessments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='综合评估表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_messages`
--

DROP TABLE IF EXISTS `chat_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_messages` (
  `id` int NOT NULL AUTO_INCREMENT,
  `session_id` int NOT NULL,
  `role` varchar(20) NOT NULL COMMENT 'user/assistant/system',
  `content` longtext NOT NULL,
  `content_format` varchar(20) NOT NULL DEFAULT 'markdown' COMMENT 'plain/markdown',
  `seq` int NOT NULL COMMENT '会话内顺序号，保证稳定排序',
  `temperature` decimal(4,3) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `chat_messages_pk` (`session_id`,`seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_sessions`
--

DROP TABLE IF EXISTS `chat_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_sessions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `scene_category_id` int DEFAULT NULL COMMENT '对应情景类别id',
  `title` varchar(100) NOT NULL DEFAULT '未命名会话',
  `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT 'active/completed/archived',
  `phase` varchar(20) NOT NULL DEFAULT 'init' COMMENT 'init/scenario/dialogue/evaluation',
  `step` int NOT NULL DEFAULT '0',
  `max_steps` int NOT NULL DEFAULT '6',
  `scenario` text COMMENT '会话实际使用的情景（AI生成后的具体情景)',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `choice_questions`
--

DROP TABLE IF EXISTS `choice_questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `choice_questions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `question_id` int DEFAULT NULL,
  `option_A` text NOT NULL,
  `option_B` text NOT NULL,
  `option_C` text NOT NULL,
  `option_D` text NOT NULL,
  `is_multiple` tinyint(1) DEFAULT '0',
  `correct_answer` json NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `question_id` (`question_id`),
  CONSTRAINT `choice_questions_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course_category`
--

DROP TABLE IF EXISTS `course_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_category` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course_favorite`
--

DROP TABLE IF EXISTS `course_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_favorite` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `course_id` int NOT NULL COMMENT '课程ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_favorite_user_course` (`user_id`,`course_id`),
  KEY `idx_course_favorite_user_id` (`user_id`),
  KEY `idx_course_favorite_course_id` (`course_id`),
  CONSTRAINT `fk_course_favorite_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course_learning`
--

DROP TABLE IF EXISTS `course_learning`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_learning` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `course_id` int NOT NULL COMMENT '课程ID',
  `status` tinyint DEFAULT '0' COMMENT '学习状态：0未开始，1学习中，2已完成',
  `progress_percent` decimal(5,2) DEFAULT '0.00' COMMENT '学习进度百分比',
  `last_sec` int DEFAULT '0' COMMENT '上次学习到的视频秒数',
  `study_total` int DEFAULT '0' COMMENT '累计学习时长，单位：秒',
  `last_time` timestamp NULL DEFAULT NULL COMMENT '最近学习时间',
  `started_at` timestamp NULL DEFAULT NULL COMMENT '开始学习时间',
  `completed_at` timestamp NULL DEFAULT NULL COMMENT '完成学习时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_learning_user_course` (`user_id`,`course_id`),
  KEY `idx_course_learning_user_id` (`user_id`),
  KEY `idx_course_learning_course_id` (`course_id`),
  KEY `idx_course_learning_status` (`status`),
  CONSTRAINT `fk_course_learning_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程学习状态表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course_note`
--

DROP TABLE IF EXISTS `course_note`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_note` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `course_id` int NOT NULL COMMENT '课程ID',
  `content` text NOT NULL COMMENT '笔记内容',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_note_user_id` (`user_id`),
  KEY `idx_course_note_course_id` (`course_id`),
  CONSTRAINT `fk_course_note_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程笔记表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course_types`
--

DROP TABLE IF EXISTS `course_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_types` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `idx_ct_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `course_user_learn`
--

DROP TABLE IF EXISTS `course_user_learn`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_user_learn` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `course_id` int NOT NULL COMMENT '课程ID',
  `learn_date` date NOT NULL COMMENT '学习日期',
  `start_time` timestamp NULL DEFAULT NULL COMMENT '本次学习开始时间',
  `end_time` timestamp NULL DEFAULT NULL COMMENT '本次学习结束时间',
  `study_sec` int DEFAULT '0' COMMENT '本次学习时长，单位：秒',
  `progress_before` decimal(5,2) DEFAULT '0.00' COMMENT '本次学习前进度',
  `progress_after` decimal(5,2) DEFAULT '0.00' COMMENT '本次学习后进度',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_user_learn_user_id` (`user_id`),
  KEY `idx_course_user_learn_course_id` (`course_id`),
  KEY `idx_course_user_learn_learn_date` (`learn_date`),
  CONSTRAINT `fk_course_user_learn_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程学习记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `courses`
--

DROP TABLE IF EXISTS `courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `courses` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  `category_id` int NOT NULL COMMENT '所属分类ID',
  `title` varchar(100) NOT NULL COMMENT '课程标题',
  `description` varchar(500) DEFAULT NULL COMMENT '课程简介',
  `cover_url` varchar(255) DEFAULT NULL COMMENT '课程封面地址',
  `video_url` varchar(255) NOT NULL COMMENT '课程视频地址',
  `difficulty` tinyint DEFAULT '1' COMMENT '难度：1初级，2中级，3高级',
  `duration` int DEFAULT '0' COMMENT '课程总时长，单位：秒',
  `teacher_type` tinyint DEFAULT NULL COMMENT '适用教师类型：0教学型，1科研型，2综合型；为空表示通用',
  `enrollment_count` int DEFAULT '0' COMMENT '学习人数',
  `hot_score` decimal(10,2) DEFAULT '0.00' COMMENT '热度值',
  `is_featured` tinyint DEFAULT '0' COMMENT '是否推荐：1是，0否',
  `status` tinyint DEFAULT '1' COMMENT '状态：1上架，0下架',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_courses_category_id` (`category_id`),
  KEY `idx_courses_teacher_type` (`teacher_type`),
  KEY `idx_courses_status` (`status`),
  KEY `idx_courses_featured` (`is_featured`),
  KEY `idx_courses_hot_score` (`hot_score`),
  CONSTRAINT `fk_course_category` FOREIGN KEY (`category_id`) REFERENCES `course_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `departments`
--

DROP TABLE IF EXISTS `departments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `departments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `disciplines`
--

DROP TABLE IF EXISTS `disciplines`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `disciplines` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) NOT NULL COMMENT '学科名称，如计算机、法学、医学',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0停用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `idx_disciplines_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学科表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `element_favorite`
--

DROP TABLE IF EXISTS `element_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `element_favorite` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '用户ID',
  `element_id` int NOT NULL COMMENT '思政元素ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_element_favorite` (`user_id`,`element_id`),
  KEY `idx_element_favorite_user_id` (`user_id`),
  KEY `idx_element_favorite_element_id` (`element_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='思政元素收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `element_types`
--

DROP TABLE IF EXISTS `element_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `element_types` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `idx_et_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `elements`
--

DROP TABLE IF EXISTS `elements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `elements` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(128) NOT NULL COMMENT '元素标题',
  `summary` varchar(255) DEFAULT NULL COMMENT '摘要',
  `content` text NOT NULL COMMENT '元素详细内容',
  `element_type_id` int NOT NULL COMMENT '元素类型ID',
  `difficulty` tinyint NOT NULL DEFAULT '2' COMMENT '难度：1初级，2中级，3高级',
  `keywords` varchar(500) DEFAULT NULL COMMENT '关键词，逗号分隔',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览次数',
  `favorite_count` int NOT NULL DEFAULT '0' COMMENT '收藏次数',
  `use_count` int NOT NULL DEFAULT '0' COMMENT '使用次数',
  `hot_score` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '热度分值',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0停用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_elements_title` (`title`),
  KEY `idx_elements_element_type_id` (`element_type_id`),
  KEY `idx_elements_difficulty` (`difficulty`),
  KEY `idx_elements_status` (`status`),
  KEY `idx_elements_hot_score` (`hot_score`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='思政元素表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `elements_course_types`
--

DROP TABLE IF EXISTS `elements_course_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `elements_course_types` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `element_id` int NOT NULL COMMENT '思政元素ID',
  `course_type_id` int NOT NULL COMMENT '课程类型ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_element_course_type` (`element_id`,`course_type_id`),
  KEY `idx_elements_course_types_element_id` (`element_id`),
  KEY `idx_elements_course_types_course_type_id` (`course_type_id`)
) ENGINE=InnoDB AUTO_INCREMENT=88 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='思政元素-课程类型关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `elements_disciplines`
--

DROP TABLE IF EXISTS `elements_disciplines`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `elements_disciplines` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `element_id` int NOT NULL COMMENT '思政元素ID',
  `discipline_id` int NOT NULL COMMENT '学科ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_element_discipline` (`element_id`,`discipline_id`),
  KEY `idx_elements_disciplines_element_id` (`element_id`),
  KEY `idx_elements_disciplines_discipline_id` (`discipline_id`)
) ENGINE=InnoDB AUTO_INCREMENT=88 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='思政元素-学科关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `elements_teacher_types`
--

DROP TABLE IF EXISTS `elements_teacher_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `elements_teacher_types` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `element_id` int NOT NULL COMMENT '思政元素ID',
  `teacher_type` tinyint NOT NULL COMMENT '教师类型：0教学型，1科研型，2综合型',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_element_teacher_type` (`element_id`,`teacher_type`),
  KEY `idx_elements_teacher_types_element_id` (`element_id`),
  KEY `idx_elements_teacher_types_teacher_type` (`teacher_type`)
) ENGINE=InnoDB AUTO_INCREMENT=91 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='思政元素-教师类型关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `elements_teaching_courses`
--

DROP TABLE IF EXISTS `elements_teaching_courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `elements_teaching_courses` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `element_id` int NOT NULL COMMENT '思政元素ID',
  `teaching_course_id` int NOT NULL COMMENT '教学课程ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_element_teaching_course` (`element_id`,`teaching_course_id`),
  KEY `idx_elements_teaching_courses_element_id` (`element_id`),
  KEY `idx_elements_teaching_courses_teaching_course_id` (`teaching_course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=92 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='思政元素-适用课程关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `essay_questions`
--

DROP TABLE IF EXISTS `essay_questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `essay_questions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `question_id` int DEFAULT NULL,
  `reference` text NOT NULL,
  `keyword` text NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `question_id` (`question_id`),
  CONSTRAINT `essay_questions_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `evaluation_dimension_scores`
--

DROP TABLE IF EXISTS `evaluation_dimension_scores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation_dimension_scores` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '得分明细ID',
  `dimension_id` int NOT NULL COMMENT '维度ID',
  `evaluation_id` int NOT NULL COMMENT '对应的评估ID',
  `score` int NOT NULL COMMENT '维度得分（0-100）',
  `weight` decimal(5,2) DEFAULT NULL COMMENT '维度权重',
  `comment` text COMMENT '该维度评语',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `fk_evaluation_dimension_scores_dimension` (`dimension_id`),
  KEY `fk_evaluation_dimension_scores_evaluation` (`evaluation_id`),
  CONSTRAINT `fk_evaluation_dimension_scores_dimension` FOREIGN KEY (`dimension_id`) REFERENCES `evaluation_dimensions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_evaluation_dimension_scores_evaluation` FOREIGN KEY (`evaluation_id`) REFERENCES `evaluations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='决策评估得分明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `evaluation_dimensions`
--

DROP TABLE IF EXISTS `evaluation_dimensions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation_dimensions` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '维度ID',
  `name` varchar(128) NOT NULL COMMENT '维度名称',
  `type` tinyint NOT NULL COMMENT '0主维度 1过程维度',
  `description` varchar(255) DEFAULT NULL COMMENT '维度描述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='决策评估维度表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `evaluation_evidences`
--

DROP TABLE IF EXISTS `evaluation_evidences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation_evidences` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '证据ID',
  `item_id` int NOT NULL COMMENT '对应的问题项ID',
  `evaluation_id` int NOT NULL COMMENT '冗余一列便于查询',
  `message_id` int DEFAULT NULL COMMENT '关联消息ID',
  `reason` text COMMENT '为什么把这段内容作为证据',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `fk_evaluation_evidences_item` (`item_id`),
  KEY `fk_evaluation_evidences_evaluation` (`evaluation_id`),
  KEY `fk_evaluation_evidences_message` (`message_id`),
  CONSTRAINT `fk_evaluation_evidences_evaluation` FOREIGN KEY (`evaluation_id`) REFERENCES `evaluations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_evaluation_evidences_item` FOREIGN KEY (`item_id`) REFERENCES `evaluation_items` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_evaluation_evidences_message` FOREIGN KEY (`message_id`) REFERENCES `chat_messages` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='决策评估证据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `evaluation_items`
--

DROP TABLE IF EXISTS `evaluation_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation_items` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '问题项ID',
  `evaluation_id` int NOT NULL COMMENT '对应的评估ID',
  `type` tinyint NOT NULL COMMENT '类型：0优势 1风险 2关键节点',
  `dimension_id` int DEFAULT NULL COMMENT '对应维度ID，可为空',
  `content` text COMMENT '内容',
  `round_no` int DEFAULT NULL COMMENT '对应轮次',
  `level` tinyint DEFAULT '3' COMMENT '等级（1高 2中 3低）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `fk_evaluation_items_evaluation` (`evaluation_id`),
  KEY `fk_evaluation_items_dimension` (`dimension_id`),
  CONSTRAINT `fk_evaluation_items_dimension` FOREIGN KEY (`dimension_id`) REFERENCES `evaluation_dimensions` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_evaluation_items_evaluation` FOREIGN KEY (`evaluation_id`) REFERENCES `evaluations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='决策评估问题项表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `evaluation_suggestions`
--

DROP TABLE IF EXISTS `evaluation_suggestions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluation_suggestions` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '建议ID',
  `evaluation_id` int NOT NULL COMMENT '对应的评估ID',
  `dimension_id` int DEFAULT NULL COMMENT '建议对应的维度ID',
  `type` tinyint NOT NULL COMMENT '0整体建议 1维度建议 2针对性建议',
  `title` varchar(120) NOT NULL COMMENT '建议标题',
  `content` text COMMENT '建议内容',
  `priority` tinyint DEFAULT '3' COMMENT '优先级：1高 2中 3低',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `fk_evaluation_suggestions_evaluation` (`evaluation_id`),
  KEY `fk_evaluation_suggestions_dimension` (`dimension_id`),
  CONSTRAINT `fk_evaluation_suggestions_dimension` FOREIGN KEY (`dimension_id`) REFERENCES `evaluation_dimensions` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_evaluation_suggestions_evaluation` FOREIGN KEY (`evaluation_id`) REFERENCES `evaluations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='决策评估建议表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `evaluations`
--

DROP TABLE IF EXISTS `evaluations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evaluations` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键，评估记录id',
  `session_id` int NOT NULL COMMENT '对应会话id',
  `user_id` int NOT NULL COMMENT '所属用户id',
  `status` tinyint DEFAULT '0' COMMENT '0生成中 1成功 2失败',
  `overall_score` int DEFAULT NULL COMMENT '总评分(0-100)',
  `overall_level` tinyint DEFAULT NULL COMMENT '等级(0优秀 1良好 2合格 3需改进)',
  `percentile` int DEFAULT NULL COMMENT '分位',
  `summary` text COMMENT '综合评价摘要',
  `style` tinyint DEFAULT NULL COMMENT '决策风格（0稳健型 1权衡型 2激进型）',
  `risk_level` tinyint DEFAULT NULL COMMENT '风险等级（1高 2中 3低）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `evaluations_pk` (`user_id`,`session_id`),
  KEY `evaluations_chat_sessions_id_fk` (`session_id`),
  CONSTRAINT `evaluations_chat_sessions_id_fk` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`id`),
  CONSTRAINT `evaluations_users_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='决策评估主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `judgment_questions`
--

DROP TABLE IF EXISTS `judgment_questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `judgment_questions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `question_id` int DEFAULT NULL,
  `correct_answer` tinyint(1) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `question_id` (`question_id`),
  CONSTRAINT `judgment_questions_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `question_dimensions`
--

DROP TABLE IF EXISTS `question_dimensions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_dimensions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `question_id` int NOT NULL,
  `dimension_id` int NOT NULL,
  `weight` decimal(5,2) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_dim` (`question_id`,`dimension_id`),
  KEY `idx_qd_question` (`question_id`),
  KEY `idx_qd_dim` (`dimension_id`)
) ENGINE=InnoDB AUTO_INCREMENT=141 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `questionnaires`
--

DROP TABLE IF EXISTS `questionnaires`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `questionnaires` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `scene_id` int DEFAULT NULL,
  `title` varchar(50) DEFAULT NULL,
  `question_sequence` json NOT NULL,
  `total_score` int DEFAULT NULL,
  `total_count` int DEFAULT NULL,
  `user_total_score` int DEFAULT '0',
  `status` int DEFAULT '0',
  `started_at` timestamp NULL DEFAULT NULL,
  `submitted_at` timestamp NULL DEFAULT NULL,
  `time_spent` int DEFAULT '0' COMMENT '用时（秒)',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `scene_id` (`scene_id`),
  CONSTRAINT `questionnaires_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `questionnaires_chk_1` CHECK ((`status` in (0,1,2,3)))
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `questions`
--

DROP TABLE IF EXISTS `questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `questions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `scene_id` int NOT NULL COMMENT '关联的场景ID',
  `type` int NOT NULL COMMENT '题目类型：1-判断题，2-选择题，3-简答题',
  `content` text NOT NULL COMMENT '题目内容',
  `analysis` text NOT NULL COMMENT '题目分析',
  `difficulty` int DEFAULT '3' COMMENT '题目难度层级',
  `score` int DEFAULT '10' COMMENT '题目分值',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `scene_id` (`scene_id`),
  CONSTRAINT `questions_chk_1` CHECK ((`type` in (0,1,2)))
) ENGINE=InnoDB AUTO_INCREMENT=161 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_dimension_scores`
--

DROP TABLE IF EXISTS `report_dimension_scores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_dimension_scores` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '得分明细ID',
  `dimension_id` int NOT NULL COMMENT '维度ID',
  `report_id` int NOT NULL COMMENT '报告ID',
  `score` int NOT NULL COMMENT '维度得分（0-100）',
  `raw_score` int DEFAULT NULL COMMENT '原始得分',
  `raw_total` int DEFAULT NULL COMMENT '原始满分',
  `question_count` int DEFAULT '0' COMMENT '该维度题目数量',
  `weight` decimal(5,2) DEFAULT NULL COMMENT '该维度题目数量/该报告总题数',
  `wrong_count` int DEFAULT '0' COMMENT '错题数',
  `low_count` int DEFAULT '0' COMMENT '简答低分数',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_dim` (`report_id`,`dimension_id`),
  KEY `idx_report` (`report_id`),
  KEY `fk_rds_dim` (`dimension_id`),
  CONSTRAINT `fk_rds_dim` FOREIGN KEY (`dimension_id`) REFERENCES `report_dimensions` (`id`),
  CONSTRAINT `fk_rds_report` FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='能力维度得分明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_dimensions`
--

DROP TABLE IF EXISTS `report_dimensions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_dimensions` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '维度ID',
  `name` varchar(64) NOT NULL COMMENT '维度名称',
  `description` varchar(255) DEFAULT NULL COMMENT '维度描述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='能力维度表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_evidences`
--

DROP TABLE IF EXISTS `report_evidences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_evidences` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '证据ID',
  `item_id` int NOT NULL COMMENT '对应能力/风险ID',
  `report_id` int NOT NULL COMMENT '报告ID（便于查）',
  `question_id` int NOT NULL COMMENT '题目ID（questions.id）',
  `reason` text COMMENT '为什么选这题作为证据',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report` (`report_id`),
  KEY `idx_sr` (`item_id`),
  KEY `idx_question` (`question_id`),
  CONSTRAINT `fk_ev_report` FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ev_sr` FOREIGN KEY (`item_id`) REFERENCES `report_items` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='证据题表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_items`
--

DROP TABLE IF EXISTS `report_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_items` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '能力/风险ID',
  `report_id` int NOT NULL COMMENT '报告ID（reports.id）',
  `type` tinyint NOT NULL COMMENT '类型：1优势 2风险',
  `content` text COMMENT '优势/风险内容',
  `level` tinyint DEFAULT '3' COMMENT '等级（1高2中3低）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_type` (`report_id`,`type`),
  CONSTRAINT `fk_sr_report` FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='能力优势/风险表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_suggestions`
--

DROP TABLE IF EXISTS `report_suggestions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_suggestions` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '建议ID',
  `report_id` int NOT NULL COMMENT '报告ID（reports.id）',
  `dimension_id` int NOT NULL COMMENT '建议对应的维度ID',
  `title` varchar(120) NOT NULL COMMENT '建议标题',
  `content` text COMMENT '建议内容',
  `priority` tinyint DEFAULT '3' COMMENT '优先级（1高2中3低）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report` (`report_id`),
  KEY `idx_dim` (`dimension_id`),
  CONSTRAINT `fk_sug_dim` FOREIGN KEY (`dimension_id`) REFERENCES `report_dimensions` (`id`),
  CONSTRAINT `fk_sug_report` FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='能力发展建议表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reports`
--

DROP TABLE IF EXISTS `reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '报告ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `questionnaire_id` int NOT NULL COMMENT '答卷ID（questionnaires.id）',
  `scene_id` int DEFAULT NULL COMMENT '情景ID（scenes.id）',
  `status` tinyint DEFAULT '0' COMMENT '0生成中 1成功 2失败',
  `total_score` int DEFAULT NULL,
  `user_total_score` int DEFAULT NULL,
  `time_spent` int DEFAULT '0',
  `overall_score` int DEFAULT NULL COMMENT '综合得分（0-100）',
  `overall_level` tinyint DEFAULT NULL COMMENT '等级（0优秀1良好2合格3需改进）',
  `percentile` int DEFAULT NULL COMMENT '分位（后续）',
  `summary` text COMMENT '总结',
  `raw_json` json DEFAULT NULL COMMENT '原始JSON（LLM输出快照）',
  `code` varchar(64) DEFAULT NULL COMMENT '报告编号',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_qn` (`user_id`,`questionnaire_id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_qn` (`questionnaire_id`),
  KEY `idx_scene` (`scene_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='测试报告总表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `scene_category`
--

DROP TABLE IF EXISTS `scene_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scene_category` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `description` varchar(200) DEFAULT NULL COMMENT '分类描述',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `scenes`
--

DROP TABLE IF EXISTS `scenes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scenes` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL COMMENT '情景标题',
  `description` text NOT NULL COMMENT '情景描述',
  `img_url` varchar(500) DEFAULT NULL COMMENT '情景图片',
  `difficulty` tinyint DEFAULT '2' COMMENT '难度等级：1-简单，2-中等，3-困难',
  `estimated_time` int DEFAULT '15' COMMENT '预计用时(分钟)',
  `participants` int DEFAULT '100' COMMENT '参与人数',
  `category_id` int NOT NULL COMMENT '场景分类ID',
  `focus` text NOT NULL,
  `analysis` text NOT NULL,
  `correct_approach` text NOT NULL,
  `incorrect_approach` text NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `teaching_courses`
--

DROP TABLE IF EXISTS `teaching_courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teaching_courses` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(128) NOT NULL COMMENT '课程名称，如人工智能基础、数据库原理',
  `discipline_id` int NOT NULL COMMENT '所属学科ID',
  `course_type_id` int NOT NULL COMMENT '课程类型ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0停用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_teaching_courses_name` (`name`),
  KEY `idx_teaching_courses_discipline_id` (`discipline_id`),
  KEY `idx_teaching_courses_course_type_id` (`course_type_id`),
  KEY `idx_teaching_courses_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教学课程表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `test_templates`
--

DROP TABLE IF EXISTS `test_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_templates` (
  `id` int NOT NULL AUTO_INCREMENT,
  `scene_id` int NOT NULL,
  `title` varchar(100) DEFAULT NULL,
  `question_sequence` json NOT NULL,
  `total_score` int NOT NULL,
  `total_count` int NOT NULL DEFAULT '20',
  `raw_json` json DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_id` (`scene_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(7) NOT NULL,
  `password` varchar(80) NOT NULL,
  `real_name` varchar(20) NOT NULL,
  `sex` int NOT NULL COMMENT '1男 0女',
  `identity_card` varchar(18) NOT NULL,
  `birthday` date NOT NULL,
  `email` varchar(100) NOT NULL,
  `phone` varchar(11) NOT NULL,
  `pic` varchar(255) DEFAULT '/avatars/default.jpg',
  `dep_id` int DEFAULT NULL,
  `title` varchar(50) DEFAULT NULL,
  `type` int DEFAULT '2' COMMENT '0-科研型，1-教学型，2-混合型',
  `hire_date` date DEFAULT NULL,
  `expertise` text,
  `bio` text,
  `discipline_id` int DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `identity_card` (`identity_card`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `phone` (`phone`),
  KEY `dep_id` (`dep_id`),
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`dep_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping events for database 'teacher_ethics'
--

--
-- Dumping routines for database 'teacher_ethics'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-14 16:12:27
