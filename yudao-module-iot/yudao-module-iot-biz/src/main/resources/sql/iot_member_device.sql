-- ==========================================
-- IoT 会员-设备绑定表（手机 App 扫码绑定外部设备）
-- 说明：iot 模块的生产建表脚本未集中维护，故本表 DDL 独立提供，
--       部署时请在目标库手动执行。
-- ==========================================
CREATE TABLE `iot_member_device` (
  `id`          bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `member_id`   bigint        NOT NULL COMMENT '会员编号',
  `mobile`      varchar(20)   NOT NULL DEFAULT '' COMMENT '会员手机号（冗余）',
  `device_id`   bigint        NOT NULL COMMENT 'IoT 设备编号',
  `product_key` varchar(100)  NOT NULL DEFAULT '' COMMENT '产品标识（冗余）',
  `device_name` varchar(255)  NOT NULL DEFAULT '' COMMENT '设备名称（冗余）',
  `nickname`    varchar(255)  DEFAULT NULL COMMENT '设备备注名',
  `creator`     varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`   bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_device_id` (`device_id`, `deleted`) USING BTREE COMMENT '一对一：一个设备仅归属一个会员',
  KEY `idx_member_id` (`member_id`) USING BTREE,
  KEY `idx_mobile` (`mobile`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'IoT 会员-设备绑定表';
