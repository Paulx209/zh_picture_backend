# -- 用户表
# create table if not exists user
# (
#     id           bigint auto_increment comment 'id' primary key,
#     userAccount  varchar(256)                           not null comment '账号',
#     userPassword varchar(512)                           not null comment '密码',
#     userName     varchar(256)                           null comment '用户昵称',
#     userAvatar   varchar(1024)                          null comment '用户头像',
#     userProfile  varchar(512)                           null comment '用户简介',
#     userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
#     editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
#     createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
#     updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
#     isDelete     tinyint      default 0                 not null comment '是否删除',
#     UNIQUE KEY uk_userAccount (userAccount),
#     INDEX idx_userName (userName)
#     ) comment '用户' collate = utf8mb4_unicode_ci;
#
#
# -- 图片表
# -- 图片表
# create table if not exists picture
# (
#     id           bigint auto_increment comment 'id' primary key,
#     url          varchar(512)                       not null comment '图片 url',
#     name         varchar(128)                       not null comment '图片名称',
#     introduction varchar(512)                       null comment '简介',
#     category     varchar(64)                        null comment '分类',
#     tags         varchar(512)                      null comment '标签（JSON 数组）',
#     picSize      bigint                             null comment '图片体积',
#     picWidth     int                                null comment '图片宽度',
#     picHeight    int                                null comment '图片高度',
#     picScale     double                             null comment '图片宽高比例',
#     picFormat    varchar(32)                        null comment '图片格式',
#     userId       bigint                             not null comment '创建用户 id',
#     createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
#     editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
#     updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
#     isDelete     tinyint  default 0                 not null comment '是否删除',
#     INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
#     INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
#     INDEX idx_category (category),         -- 提升基于分类的查询性能
#     INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
#     INDEX idx_userId (userId)              -- 提升基于用户 ID 的查询性能
# ) comment '图片' collate = utf8mb4_unicode_ci;
#
#
# ALTER TABLE picture
#     -- 添加新列
#     ADD COLUMN thumbnailUrl varchar(512) NULL COMMENT '缩略图 url';
#
# #添加reviewStatus 那些xxx
#
#
# -- 空间表
# create table if not exists space
# (
#     id         bigint auto_increment comment 'id' primary key,
#     spaceName  varchar(128)                       null comment '空间名称',
#     spaceLevel int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
#     maxSize    bigint   default 0                 null comment '空间图片的最大总大小',
#     maxCount   bigint   default 0                 null comment '空间图片的最大数量',
#     totalSize  bigint   default 0                 null comment '当前空间下图片的总大小',
#     totalCount bigint   default 0                 null comment '当前空间下的图片数量',
#     userId     bigint                             not null comment '创建用户 id',
#     createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
#     editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
#     updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
#     isDelete   tinyint  default 0                 not null comment '是否删除',
#     -- 索引设计
#     index idx_userId (userId),        -- 提升基于用户的查询效率
#     index idx_spaceName (spaceName),  -- 提升基于空间名称的查询效率
#     index idx_spaceLevel (spaceLevel) -- 提升按空间级别查询的效率
# ) comment '空间' collate = utf8mb4_unicode_ci;
#
# #因为有了空间表 每一个图片都要对应一个空间(公共空间或者私有空间)
# ALTER TABLE picture
#     ADD COLUMN spaceId  bigint  null comment '空间 id（为空表示公共空间）';
#
# -- 创建索引
# CREATE INDEX idx_spaceId ON picture (spaceId);




