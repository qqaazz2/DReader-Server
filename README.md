# DReader-Server

DReader 的后台服务，一个轻量级 EPUB 小说阅读器的服务端。

## 项目简介

DReader-Server 是 [DReader](https://github.com/qqaazz2/DReader) 的服务端程序，基于 Spring Boot 构建，旨在提供一个私有化、可自托管的 EPUB 书库管理与同步方案。它支持书籍存储、元数据解析与管理，并负责在不同设备间无缝同步阅读进度，是 DReader 客户端的核心后台支持。

## 核心功能

- 📂书籍管理: 支持 EPUB 文件的存储与分类，自动解析元数据（如标题、作者、封面、简介等），并建立书籍与系列的关联。
- 🔍目录扫描: 可通过手动触发，扫描指定目录下的 EPUB 文件，自动识别并入库，保持书库最新。
- 🔄阅读进度同步: 负责保存并同步用户在各设备上的阅读进度，实现真正的多端无缝衔接。
- 🛠   自托管服务: 基于 Spring Boot 构建，轻量化部署，支持私有化书库管理，数据完全可控。
- 🔑用户登录: 提供基础的用户登录功能，保障个人书库的私密性。

## 技术栈

- 💻后端开发: Java 17 · Spring Boot 3
- 🗄️数据库: MySQL · MyBatis-Plus
- ⚡缓存与会话: Redis
- 🔐认证机制: Spring Security · 自定义 JWT · 无状态认证
- 🛠️构建工具: Maven
- 📦部署方式: Docker · Docker Compose

## 开发环境设置

### 1. 环境要求

- JDK 17
- Maven 3.9+
- MySQL 8.x
- Redis 7.x
- Docker / Docker Compose（可选，用于容器化部署）

### 2. 初始步骤

```bash
# 克隆项目
git clone https://github.com/qqaazz2/DReader-Server.git
cd DReader-Server

# 构建项目
mvn clean package

# 运行（需要先启动 MySQL 和 Redis）
java -jar target/dreader-server-0.0.1-SNAPSHOT.jar
```

## 待实现功能

### 🚀 功能规划

- ⏱️ 阅读时长统计：自动记录并分析用户的阅读时长
- 📂 文件管理优化：支持本地文件阅读与上传，实现统一管理
- 🏷️ 标签管理功能：系列以及书籍标签

## 许可证

MIT
