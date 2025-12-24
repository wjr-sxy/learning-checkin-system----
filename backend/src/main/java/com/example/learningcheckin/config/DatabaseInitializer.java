package com.example.learningcheckin.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Checking database schema...");
        
        try {
            // Re-create table to ensure schema correctness
            jdbcTemplate.execute("DROP TABLE IF EXISTS `sys_config`");
            
            jdbcTemplate.execute("CREATE TABLE `sys_config` (\n" +
                    "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "    `config_key` VARCHAR(100) NOT NULL UNIQUE COMMENT 'Config Key',\n" +
                    "    `config_value` VARCHAR(255) DEFAULT NULL COMMENT 'Config Value',\n" +
                    "    `description` VARCHAR(255) DEFAULT NULL COMMENT 'Description',\n" +
                    "    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
                    "    PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System Config'");
             System.out.println("Created sys_config table.");
             
             // Insert default points multiplier
             try {
                 jdbcTemplate.execute("INSERT INTO `sys_config` (config_key, config_value, description) VALUES " +
                         "('points_multiplier', '1.5', 'Global Points Multiplier (0.1-5.0)')");
                 System.out.println("Inserted default points_multiplier.");
             } catch (Exception e) {
                 e.printStackTrace();
             }
        } catch (Exception e) {
             e.printStackTrace();
        }

        try {
            jdbcTemplate.execute("ALTER TABLE `sys_study_plan` ADD COLUMN `total_tasks` INT DEFAULT 0 COMMENT 'Total Tasks'");
            System.out.println("Added total_tasks column.");
        } catch (Exception e) {
            // Ignore if exists
        }
        try {
            jdbcTemplate.execute("ALTER TABLE `sys_study_plan` ADD COLUMN `completed_tasks` INT DEFAULT 0 COMMENT 'Completed Tasks'");
             System.out.println("Added completed_tasks column.");
        } catch (Exception e) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE `sys_study_plan` ADD COLUMN `progress_percentage` DECIMAL(5,1) DEFAULT 0.0 COMMENT 'Progress Percentage'");
             System.out.println("Added progress_percentage column.");
        } catch (Exception e) {
        }
        
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_study_plan_progress_history` (\n" +
                    "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "    `plan_id` BIGINT NOT NULL COMMENT 'Study Plan ID',\n" +
                    "    `previous_progress` DECIMAL(5,1) DEFAULT 0.0 COMMENT 'Previous Progress',\n" +
                    "    `new_progress` DECIMAL(5,1) NOT NULL COMMENT 'New Progress',\n" +
                    "    `completed_tasks` INT DEFAULT 0 COMMENT 'Completed Tasks at that time',\n" +
                    "    `total_tasks` INT DEFAULT 0 COMMENT 'Total Tasks at that time',\n" +
                    "    `note` VARCHAR(255) DEFAULT NULL COMMENT 'Progress Note/Hint',\n" +
                    "    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Study Plan Progress History'");
             System.out.println("Created history table.");
        } catch (Exception e) {
             e.printStackTrace();
        }
        
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_study_plan_task` (\n" +
                    "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "    `plan_id` BIGINT NOT NULL COMMENT 'Study Plan ID',\n" +
                    "    `title` VARCHAR(255) NOT NULL COMMENT 'Task Title',\n" +
                    "    `description` TEXT DEFAULT NULL COMMENT 'Task Description',\n" +
                    "    `standard` VARCHAR(255) DEFAULT NULL COMMENT 'Completion Standard',\n" +
                    "    `priority` INT DEFAULT 0 COMMENT 'Priority',\n" +
                    "    `deadline` DATE DEFAULT NULL COMMENT 'Deadline',\n" +
                    "    `status` INT DEFAULT 0 COMMENT '0: Pending, 1: Completed',\n" +
                    "    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Study Plan Tasks'");
             System.out.println("Created study plan task table.");
        } catch (Exception e) {
             e.printStackTrace();
        }
        
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_points_record` (\n" +
                    "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "    `user_id` BIGINT NOT NULL COMMENT 'User ID',\n" +
                    "    `type` INT NOT NULL COMMENT '1: Gain, 2: Consume',\n" +
                    "    `amount` INT NOT NULL COMMENT 'Points Amount',\n" +
                    "    `description` VARCHAR(255) DEFAULT NULL COMMENT 'Description',\n" +
                    "    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Points Record'");
             System.out.println("Created points record table.");
        } catch (Exception e) {
             e.printStackTrace();
        }
        
        // New columns for User: continuous_checkin_days, last_checkin_date
        try {
            jdbcTemplate.execute("ALTER TABLE `sys_user` ADD COLUMN `continuous_checkin_days` INT DEFAULT 0 COMMENT 'Continuous Check-in Days'");
             System.out.println("Added continuous_checkin_days column.");
        } catch (Exception e) { }

        try {
            jdbcTemplate.execute("ALTER TABLE `sys_user` ADD COLUMN `last_checkin_date` DATE DEFAULT NULL COMMENT 'Last Check-in Date'");
             System.out.println("Added last_checkin_date column.");
        } catch (Exception e) { }
        
        // New column for Checkin: is_supplementary
        try {
            jdbcTemplate.execute("ALTER TABLE `sys_checkin` ADD COLUMN `is_supplementary` BOOLEAN DEFAULT FALSE COMMENT 'Is Supplementary Check-in'");
             System.out.println("Added is_supplementary column.");
        } catch (Exception e) { }
        
        // New column for Order: status
        try {
            jdbcTemplate.execute("ALTER TABLE `sys_order` ADD COLUMN `status` INT DEFAULT 0 COMMENT '0: Success, 1: Refunded'");
             System.out.println("Added status column to sys_order.");
        } catch (Exception e) { }

        try {
            jdbcTemplate.execute("ALTER TABLE `sys_order` ADD COLUMN `is_abnormal` BOOLEAN DEFAULT FALSE COMMENT 'Is Abnormal'");
             System.out.println("Added is_abnormal column to sys_order.");
        } catch (Exception e) { }

        // Course Tables
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_course` (\n" +
                    "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "    `teacher_id` BIGINT NOT NULL COMMENT 'Teacher ID',\n" +
                    "    `name` VARCHAR(255) NOT NULL COMMENT 'Course Name',\n" +
                    "    `description` TEXT DEFAULT NULL COMMENT 'Description',\n" +
                    "    `code` VARCHAR(50) NOT NULL COMMENT 'Join Code',\n" +
                    "    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Courses'");
             System.out.println("Created course table.");
        } catch (Exception e) {
             e.printStackTrace();
        }

        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_course_student` (\n" +
                    "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "    `course_id` BIGINT NOT NULL COMMENT 'Course ID',\n" +
                    "    `student_id` BIGINT NOT NULL COMMENT 'Student ID',\n" +
                    "    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    PRIMARY KEY (`id`),\n" +
                    "    UNIQUE KEY `uk_course_student` (`course_id`, `student_id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Course Students'");
             System.out.println("Created course_student table.");
        } catch (Exception e) {
             e.printStackTrace();
        }

        // User status
        try {
            jdbcTemplate.execute("ALTER TABLE `sys_user` ADD COLUMN `status` INT DEFAULT 0 COMMENT '0: Normal, 1: Banned'");
            System.out.println("Added status column to sys_user.");
        } catch (Exception e) { }

        // Operation Log
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_operation_log` (\n" +
                    "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "    `user_id` BIGINT DEFAULT NULL COMMENT 'User ID',\n" +
                    "    `module` VARCHAR(50) DEFAULT NULL COMMENT 'Module',\n" +
                    "    `action` VARCHAR(50) DEFAULT NULL COMMENT 'Action',\n" +
                    "    `description` VARCHAR(255) DEFAULT NULL COMMENT 'Description',\n" +
                    "    `method` VARCHAR(10) DEFAULT NULL COMMENT 'Request Method',\n" +
                    "    `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP Address',\n" +
                    "    `status` INT DEFAULT 0 COMMENT '0: Success, 1: Fail',\n" +
                    "    `execution_time` BIGINT DEFAULT 0 COMMENT 'Execution Time (ms)',\n" +
                    "    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Operation Logs'");
            System.out.println("Created sys_operation_log table.");
        } catch (Exception e) { e.printStackTrace(); }

        // Points Rule
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_points_rule` (\n" +
                    "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "    `rule_key` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Rule Key',\n" +
                    "    `rule_value` VARCHAR(255) NOT NULL COMMENT 'Rule Value',\n" +
                    "    `description` VARCHAR(255) DEFAULT NULL COMMENT 'Description',\n" +
                    "    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
                    "    PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Points Rules'");
            System.out.println("Created sys_points_rule table.");
            
            // Insert default rules if not exist
            // daily_checkin: 10, streak_bonus: 5, makeup_cost: 50
            try {
                jdbcTemplate.execute("INSERT IGNORE INTO `sys_points_rule` (rule_key, rule_value, description) VALUES " +
                        "('daily_checkin', '10', 'Daily Check-in Base Points')," +
                        "('streak_bonus', '5', 'Continuous Check-in Bonus')," +
                        "('makeup_cost', '50', 'Makeup Check-in Cost')");
            } catch (Exception e) {}
            
        } catch (Exception e) { e.printStackTrace(); }

        // Announcement
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_announcement` (\n" +
                    "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "    `title` VARCHAR(255) NOT NULL COMMENT 'Title',\n" +
                    "    `content` TEXT DEFAULT NULL COMMENT 'Content',\n" +
                    "    `status` INT DEFAULT 0 COMMENT '0: Draft, 1: Published',\n" +
                    "    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Announcements'");
            System.out.println("Created sys_announcement table.");
        } catch (Exception e) { e.printStackTrace(); }

        // Login Log
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_login_log` (\n" +
                    "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "    `user_id` BIGINT DEFAULT NULL COMMENT 'User ID',\n" +
                    "    `username` VARCHAR(50) DEFAULT NULL COMMENT 'Username',\n" +
                    "    `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP Address',\n" +
                    "    `status` INT DEFAULT 0 COMMENT '0: Success, 1: Fail',\n" +
                    "    `message` VARCHAR(255) DEFAULT NULL COMMENT 'Message',\n" +
                    "    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Login Logs'");
            System.out.println("Created sys_login_log table.");
        } catch (Exception e) { e.printStackTrace(); }

        // Blacklist
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_blacklist` (\n" +
                    "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "    `type` VARCHAR(50) NOT NULL COMMENT 'Type: JWT, IP, SENSITIVE_WORD',\n" +
                    "    `value` VARCHAR(255) NOT NULL COMMENT 'Value',\n" +
                    "    `reason` VARCHAR(255) DEFAULT NULL COMMENT 'Reason',\n" +
                    "    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Blacklist'");
            System.out.println("Created sys_blacklist table.");
        } catch (Exception e) { e.printStackTrace(); }


        
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_task_checkin` (\n" +
                    "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "    `task_id` BIGINT NOT NULL,\n" +
                    "    `student_id` BIGINT NOT NULL,\n" +
                    "    `date` DATE NOT NULL,\n" +
                    "    `checkin_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    `content` TEXT DEFAULT NULL,\n" +
                    "    `file_url` VARCHAR(1024) DEFAULT NULL,\n" +
                    "    `is_makeup` BOOLEAN DEFAULT FALSE,\n" +
                    "    `points_awarded` INT DEFAULT 0,\n" +
                    "    PRIMARY KEY (`id`),\n" +
                    "    UNIQUE KEY `uk_task_student_date` (`task_id`, `student_id`, `date`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Task Daily Checkins'");
            System.out.println("Created sys_task_checkin table.");
        } catch (Exception e) { e.printStackTrace(); }
        
        // --- Personal Center Updates ---
        try {
            jdbcTemplate.execute("ALTER TABLE `sys_user` MODIFY COLUMN `avatar` LONGTEXT DEFAULT NULL COMMENT 'Avatar'");
            System.out.println("Modified avatar column to LONGTEXT.");
        } catch (Exception e) {
             // Ignore if fails (e.g. column not exists, though unlikely for avatar)
             e.printStackTrace();
        }

        try {
            jdbcTemplate.execute("ALTER TABLE `sys_user` ADD COLUMN `full_name` VARCHAR(50) DEFAULT NULL COMMENT 'Full Name'");
            jdbcTemplate.execute("ALTER TABLE `sys_user` ADD COLUMN `college` VARCHAR(100) DEFAULT NULL COMMENT 'College'");
            jdbcTemplate.execute("ALTER TABLE `sys_user` ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL COMMENT 'Phone Number'");
            jdbcTemplate.execute("ALTER TABLE `sys_user` ADD COLUMN `current_avatar_frame` VARCHAR(255) DEFAULT NULL COMMENT 'Current Avatar Frame'");
            jdbcTemplate.execute("ALTER TABLE `sys_user` ADD COLUMN `current_skin` VARCHAR(255) DEFAULT NULL COMMENT 'Current Skin'");
            jdbcTemplate.execute("ALTER TABLE `sys_user` ADD COLUMN `current_badge` VARCHAR(255) DEFAULT NULL COMMENT 'Current Badge'");
            System.out.println("Added personal center columns to sys_user.");
        } catch (Exception e) {
             // Ignore if exists
        }

        try {
            jdbcTemplate.execute("ALTER TABLE `sys_login_log` ADD COLUMN `device` VARCHAR(255) DEFAULT NULL COMMENT 'Device info/User Agent'");
            System.out.println("Added device column to sys_login_log.");
        } catch (Exception e) {
             // Ignore if exists
        }

        // --- Online Time Stats ---
        try {
            jdbcTemplate.execute("ALTER TABLE `sys_user` ADD COLUMN `total_online_seconds` BIGINT DEFAULT 0 COMMENT 'Total Online Seconds'");
            jdbcTemplate.execute("ALTER TABLE `sys_user` ADD COLUMN `last_active_time` DATETIME DEFAULT NULL COMMENT 'Last Active Time'");
            System.out.println("Added online time columns to sys_user.");
        } catch (Exception e) { }

        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_daily_online_stats` (\n" +
                    "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "    `user_id` BIGINT NOT NULL COMMENT 'User ID',\n" +
                    "    `stats_date` DATE NOT NULL COMMENT 'Stats Date',\n" +
                    "    `duration_seconds` BIGINT DEFAULT 0 COMMENT 'Duration Seconds',\n" +
                    "    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n" +
                    "    PRIMARY KEY (`id`),\n" +
                    "    UNIQUE KEY `uk_user_date` (`user_id`, `stats_date`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Daily Online Stats'");
            System.out.println("Created sys_daily_online_stats table.");
        } catch (Exception e) { e.printStackTrace(); }

        // --- Physical Items Redemption ---
        try {
            jdbcTemplate.execute("ALTER TABLE `sys_product` ADD COLUMN `type` VARCHAR(20) DEFAULT 'VIRTUAL' COMMENT 'Product Type: VIRTUAL, PHYSICAL'");
            jdbcTemplate.execute("ALTER TABLE `sys_product` ADD COLUMN `stock` INT DEFAULT 0 COMMENT 'Stock Quantity'");
            System.out.println("Added type and stock columns to sys_product.");
        } catch (Exception e) { }

        try {
            jdbcTemplate.execute("ALTER TABLE `sys_order` ADD COLUMN `shipping_address` VARCHAR(255) DEFAULT NULL COMMENT 'Shipping Address'");
            jdbcTemplate.execute("ALTER TABLE `sys_order` ADD COLUMN `receiver_name` VARCHAR(50) DEFAULT NULL COMMENT 'Receiver Name'");
            jdbcTemplate.execute("ALTER TABLE `sys_order` ADD COLUMN `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT 'Receiver Phone'");
            jdbcTemplate.execute("ALTER TABLE `sys_order` ADD COLUMN `tracking_number` VARCHAR(100) DEFAULT NULL COMMENT 'Tracking Number'");
            jdbcTemplate.execute("ALTER TABLE `sys_order` ADD COLUMN `shipping_status` INT DEFAULT 0 COMMENT '0: Pending, 1: Shipped, 2: Delivered'");
            System.out.println("Added shipping columns to sys_order.");
        } catch (Exception e) { }

        // --- Teacher Task System ---
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_task` (\n" +
                    "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "    `teacher_id` BIGINT NOT NULL COMMENT 'Teacher User ID',\n" +
                    "    `course_id` BIGINT DEFAULT NULL COMMENT 'Course ID (Optional)',\n" +
                    "    `title` VARCHAR(255) NOT NULL COMMENT 'Task Title',\n" +
                    "    `content` LONGTEXT COMMENT 'Task Content (Rich Text)',\n" +
                    "    `attachment_url` VARCHAR(1024) DEFAULT NULL COMMENT 'Attachment URL',\n" +
                    "    `deadline` DATETIME NOT NULL COMMENT 'Deadline',\n" +
                    "    `reward_points` INT DEFAULT 0 COMMENT 'Reward Points',\n" +
                    "    `submit_type` VARCHAR(20) DEFAULT 'TEXT' COMMENT 'TEXT, FILE, IMAGE',\n" +
                    "    `status` INT DEFAULT 0 COMMENT '0: Draft, 1: Published',\n" +
                    "    `is_recurring` BOOLEAN DEFAULT FALSE COMMENT 'Is Recurring Task',\n" +
                    "    `start_date` DATE DEFAULT NULL COMMENT 'Start Date for Recurring',\n" +
                    "    `end_date` DATE DEFAULT NULL COMMENT 'End Date for Recurring',\n" +
                    "    `makeup_count` INT DEFAULT 0 COMMENT 'Allowed Makeup Count',\n" +
                "    `makeup_cost_percent` INT DEFAULT 100 COMMENT 'Makeup Cost Percentage',\n" +
                "    `content_template` TEXT COMMENT 'Content Template',\n" +
                "    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                "    PRIMARY KEY (`id`)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Teacher Tasks'");

            // Ensure columns exist for existing tables
            try {
                jdbcTemplate.execute("ALTER TABLE `sys_task` ADD COLUMN `is_recurring` BOOLEAN DEFAULT FALSE COMMENT 'Is Recurring Task'");
                System.out.println("Added column is_recurring to sys_task");
            } catch (Exception e) {
                System.out.println("Column is_recurring might already exist: " + e.getMessage());
            }
            try {
                jdbcTemplate.execute("ALTER TABLE `sys_task` ADD COLUMN `start_date` DATE DEFAULT NULL COMMENT 'Start Date for Recurring'");
                System.out.println("Added column start_date to sys_task");
            } catch (Exception e) {
                System.out.println("Column start_date might already exist: " + e.getMessage());
            }
            try {
                jdbcTemplate.execute("ALTER TABLE `sys_task` ADD COLUMN `end_date` DATE DEFAULT NULL COMMENT 'End Date for Recurring'");
                System.out.println("Added column end_date to sys_task");
            } catch (Exception e) {
                System.out.println("Column end_date might already exist: " + e.getMessage());
            }
            try {
                jdbcTemplate.execute("ALTER TABLE `sys_task` ADD COLUMN `makeup_count` INT DEFAULT 0 COMMENT 'Allowed Makeup Count'");
                System.out.println("Added column makeup_count to sys_task");
            } catch (Exception e) {
                System.out.println("Column makeup_count might already exist: " + e.getMessage());
            }
            try {
                jdbcTemplate.execute("ALTER TABLE `sys_task` ADD COLUMN `makeup_cost_percent` INT DEFAULT 100 COMMENT 'Makeup Cost Percentage'");
                System.out.println("Added column makeup_cost_percent to sys_task");
            } catch (Exception e) {
                System.out.println("Column makeup_cost_percent might already exist: " + e.getMessage());
            }
            try {
                jdbcTemplate.execute("ALTER TABLE `sys_task` ADD COLUMN `content_template` TEXT COMMENT 'Content Template'");
                System.out.println("Added column content_template to sys_task");
            } catch (Exception e) {
                System.out.println("Column content_template might already exist: " + e.getMessage());
            }

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_sensitive_log` (\n" +
                "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                "    `user_id` BIGINT NOT NULL,\n" +
                "    `content_snippet` VARCHAR(1024),\n" +
                "    `detected_words` VARCHAR(255),\n" +
                "    `source_type` VARCHAR(50) COMMENT 'TASK_SUBMISSION, COMMENT, etc.',\n" +
                "    `source_id` BIGINT,\n" +
                "    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                "    PRIMARY KEY (`id`)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Sensitive Word Logs'");
            System.out.println("Created sys_task table.");
        } catch (Exception e) { e.printStackTrace(); }

        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_task_submission` (\n" +
                    "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "    `task_id` BIGINT NOT NULL COMMENT 'Task ID',\n" +
                    "    `student_id` BIGINT NOT NULL COMMENT 'Student User ID',\n" +
                    "    `content` LONGTEXT COMMENT 'Submission Content',\n" +
                    "    `file_urls` TEXT COMMENT 'File URLs (JSON)',\n" +
                    "    `status` INT DEFAULT 0 COMMENT '0: Pending, 1: Graded, 2: Returned',\n" +
                    "    `score` INT DEFAULT 0 COMMENT 'Score/Points Awarded',\n" +
                    "    `rating` INT DEFAULT 0 COMMENT '1-5 Star Rating',\n" +
                    "    `comment` VARCHAR(1024) DEFAULT NULL COMMENT 'Teacher Comment',\n" +
                    "    `submit_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    `grade_time` DATETIME DEFAULT NULL,\n" +
                    "    PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Task Submissions'");
            System.out.println("Created sys_task_submission table.");
        } catch (Exception e) { e.printStackTrace(); }

        try {
            jdbcTemplate.execute("ALTER TABLE `sys_study_plan` ADD COLUMN `is_point_eligible` BOOLEAN DEFAULT TRUE COMMENT 'Is eligible for points'");
            System.out.println("Added is_point_eligible column to sys_study_plan.");
        } catch (Exception e) { }



    }
}
