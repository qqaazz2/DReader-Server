SET FOREIGN_KEY_CHECKS=0;

ALTER TABLE `dreaderserver`.`files_details` ADD COLUMN `scraper` tinyint(1) NULL DEFAULT NULL COMMENT '1成功 2失败' AFTER `name`;

ALTER TABLE `dreaderserver`.`user` ADD COLUMN `scraper` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Bangumi Archive' AFTER `file_adapter`;

ALTER TABLE `dreaderserver`.`user` ADD COLUMN `archive_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL AFTER `scraper`;

SET FOREIGN_KEY_CHECKS=1;