/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80031 (8.0.31)
 Source Host           : localhost:3306
 Source Schema         : dreaderserver

 Target Server Type    : MySQL
 Target Server Version : 80031 (8.0.31)
 File Encoding         : 65001

 Date: 16/12/2025 22:11:38
*/
CREATE DATABASE `dreaderserver` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `dreaderserver`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for author
-- ----------------------------
DROP TABLE IF EXISTS `author`;
CREATE TABLE `author`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '名称',
  `profile` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '简介',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图片',
  `date` date NULL DEFAULT NULL COMMENT '出生年月',
  `deleted` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1未删除 2已删除',
  `vocational` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `bgm_id` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of author
-- ----------------------------

-- ----------------------------
-- Table structure for book
-- ----------------------------
DROP TABLE IF EXISTS `book`;
CREATE TABLE `book`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '书籍ID',
  `files_id` int NOT NULL COMMENT 'filesID',
  `cover` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面ID(files_id)',
  `author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '小说作者',
  `profile` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '小说简介',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '阅读情况 1未读 2已读 3在读',
  `progress` decimal(5, 2) NOT NULL DEFAULT 0.00 COMMENT '阅读进度',
  `deleted` tinyint UNSIGNED NOT NULL DEFAULT 1,
  `publishing` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `read_tag_num` int NOT NULL DEFAULT 0 COMMENT '记录标签',
  `read_time` datetime NULL DEFAULT NULL COMMENT '上次阅读时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1512 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of book
-- ----------------------------

-- ----------------------------
-- Table structure for files
-- ----------------------------
DROP TABLE IF EXISTS `files`;
CREATE TABLE `files`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `file_name` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名称',
  `file_size` int NULL DEFAULT NULL COMMENT '文件大小',
  `file_path` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件路径',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件类型',
  `file_note` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件备注',
  `deleted` tinyint(1) NOT NULL DEFAULT 1 COMMENT '删除状态 1未删除 2已删除',
  `add_time` datetime NOT NULL COMMENT '文件创建时间',
  `is_folder` tinyint(1) NOT NULL DEFAULT 2 COMMENT '是否为文件夹 1是 2不是',
  `type` tinyint(1) NOT NULL COMMENT '文件所属 0缓存文件 1图书 2漫画',
  `parent_id` int NULL DEFAULT -1 COMMENT '父级文件夹',
  `hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件hash',
  `edit_time` datetime NULL DEFAULT NULL,
  `status` tinyint(1) NULL DEFAULT NULL,
  `cover` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `inode` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `id`(`type` ASC, `deleted` ASC, `is_folder` ASC, `parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1727 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of files
-- ----------------------------

-- ----------------------------
-- Table structure for files_author
-- ----------------------------
DROP TABLE IF EXISTS `files_author`;
CREATE TABLE `files_author`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `files_id` int NOT NULL,
  `author_id` int NOT NULL,
  `deleted` tinyint NOT NULL DEFAULT 1 COMMENT '1未删除 2已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 62 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of files_author
-- ----------------------------

-- ----------------------------
-- Table structure for files_details
-- ----------------------------
DROP TABLE IF EXISTS `files_details`;
CREATE TABLE `files_details`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文件数据状态ID',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '阅读状态 1未读 2已读 3在读',
  `over_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '完结状态 1连载中 2完结 3弃坑\r\n',
  `love` tinyint(1) NOT NULL DEFAULT 1 COMMENT '收藏状态 1未收藏 2已收藏',
  `last_read_time` datetime NULL DEFAULT NULL,
  `cover` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件封面',
  `profile` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '系列简介',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '日版原名',
  `files_id` int NOT NULL,
  `bgm_id` int NULL DEFAULT NULL,
  `date` date NULL DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT 1,
  `name` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '可修改名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 117 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of files_details
-- ----------------------------

-- ----------------------------
-- Table structure for files_tags
-- ----------------------------
DROP TABLE IF EXISTS `files_tags`;
CREATE TABLE `files_tags`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `files_id` int NOT NULL,
  `tags_id` int NOT NULL,
  `deleted` tinyint NOT NULL DEFAULT 1 COMMENT '1未删除 2已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 616 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of files_tags
-- ----------------------------

-- ----------------------------
-- Table structure for logging_event
-- ----------------------------
DROP TABLE IF EXISTS `logging_event`;
CREATE TABLE `logging_event`  (
  `event_id` bigint NOT NULL AUTO_INCREMENT,
  `timestmp` bigint NOT NULL,
  `formatted_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `logger_name` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `level_string` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `thread_name` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reference_flag` smallint NULL DEFAULT NULL,
  `arg0` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `arg1` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `arg2` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `arg3` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `caller_filename` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `caller_class` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `caller_method` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `caller_line` char(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`event_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1750 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of logging_event
-- ----------------------------

-- ----------------------------
-- Table structure for logging_event_exception
-- ----------------------------
DROP TABLE IF EXISTS `logging_event_exception`;
CREATE TABLE `logging_event_exception`  (
  `event_id` bigint NOT NULL,
  `i` smallint NOT NULL,
  `trace_line` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`event_id`, `i`) USING BTREE,
  CONSTRAINT `logging_event_exception_ibfk_1` FOREIGN KEY (`event_id`) REFERENCES `logging_event` (`event_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of logging_event_exception
-- ----------------------------

-- ----------------------------
-- Table structure for logging_event_property
-- ----------------------------
DROP TABLE IF EXISTS `logging_event_property`;
CREATE TABLE `logging_event_property`  (
  `event_id` bigint NOT NULL,
  `mapped_key` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `mapped_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  PRIMARY KEY (`event_id`, `mapped_key`) USING BTREE,
  CONSTRAINT `logging_event_property_ibfk_1` FOREIGN KEY (`event_id`) REFERENCES `logging_event` (`event_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of logging_event_property
-- ----------------------------

-- ----------------------------
-- Table structure for read_log
-- ----------------------------
DROP TABLE IF EXISTS `read_log`;
CREATE TABLE `read_log`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '阅读时长记录ID',
  `time` datetime NOT NULL COMMENT '阅读时长日期',
  `seconds` int NOT NULL DEFAULT 0 COMMENT '阅读时长',
  `deleted` tinyint(1) NOT NULL DEFAULT 1 COMMENT '删除状态',
  `files_id` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of read_log
-- ----------------------------

-- ----------------------------
-- Table structure for series
-- ----------------------------
DROP TABLE IF EXISTS `series`;
CREATE TABLE `series`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '系列ID',
  `files_id` int NOT NULL COMMENT 'files外键',
  `author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '系列作者',
  `over_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '完结状态 1连载中 2完结 3弃坑\r\n',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '阅读状态 1未读 2已读 3在读',
  `deleted` tinyint UNSIGNED NOT NULL DEFAULT 1,
  `love` tinyint(1) NOT NULL DEFAULT 1 COMMENT '收藏状态 1未收藏 2已收藏',
  `profile` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '系列简介',
  `last_read_time` datetime NULL DEFAULT NULL COMMENT '最后阅读时间',
  `num` int NOT NULL DEFAULT 0 COMMENT '系列小说数量',
  `cover` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '系列封面',
  `add_time` datetime NULL DEFAULT NULL,
  `edit_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 142 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of series
-- ----------------------------

-- ----------------------------
-- Table structure for tags
-- ----------------------------
DROP TABLE IF EXISTS `tags`;
CREATE TABLE `tags`  (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  `deleted` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1未删除 2已删除',
  `add_time` datetime NULL DEFAULT NULL,
  `edit_time` datetime NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 155 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tags
-- ----------------------------

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `email` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账号',
  `password` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `theme` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1为白色 2为黑色',
  `expire_time` datetime NULL DEFAULT NULL,
  `mystery` tinyint(1) NOT NULL DEFAULT 1 COMMENT '神秘开关状态',
  `mystery_password` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '神秘开关密码',
  `cover` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '头像',
  `file_adapter` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'system' COMMENT '文件适配器',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'EMT', '1@gmail.com', '$2a$10$wdSriZ4l/e/sA8KK0gelOuTyPdgv5.LAuvFy04zvVGkLD6FJYNUnu', 1, NULL, 2, '$2a$10$U9Tp6gDajm5E5xcwhJXO..tZIZ7KxnZ48O/T3QXeBUAPtbSVjPkuS', '/files//user/cover.jpg', 'system');

SET FOREIGN_KEY_CHECKS = 1;
