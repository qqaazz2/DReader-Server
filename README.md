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
- 📂对象存储: MinIO
- 🔐认证机制: Spring Security · 自定义 JWT · 无状态认证
- 🛠️构建工具: Maven
- 📦部署方式: Docker · Docker Compose

## 开发环境设置

### 1. 环境要求

- JDK 17
- Maven 3.9+
- MySQL 8.x
- Redis 7.x
- Minio
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
### 3. MinIO 配置
项目依赖 MinIO 作为对象存储，用于存放书籍封面等文件。
目前仅支持 MinIO 存储方式（暂不支持本地文件系统或其他云存储服务），请务必确保 MinIO 已启动。

启动 MinIO（示例，使用 Docker）
```bash
docker run -d \
  --name minio \
  -p 9000:9000 \
  -p 9001:9001 \
  -e "MINIO_ROOT_USER=admin" \
  -e "MINIO_ROOT_PASSWORD=admin123" \
  quay.io/minio/minio server /data --console-address ":9001"
```
>⚠️ MinIO 目前是存储书籍封面等文件的唯一方式，其他存储方式暂不支持，敬请谅解。

## 📂 支持的文件结构说明

当前版本的规则如下：

- ❌ **不支持**：将 EPUB 文件直接放在 `books` 根目录下，前端无法显示。  
- ✅ **必须**：在 `books` 下创建二级目录（系列），书籍文件放在系列目录下才能正常显示。  
- 📚 **系列**：每个二级目录会被识别为一个系列，里面的书籍会显示在该系列下。  
- ⚠️ **限制**：三级及以上目录目前会被**扁平化**处理，统一当作二级系列显示。  
- 🚀 **规划**： 将支持一级 / 二级 / 三级等层级系列显示
---

### 📂 目录层级树状图示意

#### ✅ 正确结构（书籍必须放在二级目录（或以下） = 系列中）

```
books
├── SeriesA # 系列 A
│ ├── book1.epub
│ └── book2.epub
│
├── SeriesB # 系列 B
│ ├── book3.epub
│ └── book4.epub
│
└── SeriesC # 系列 C
  └── SubSeries # ⚠️ 三级目录会被当作二级系列显示
├── book5.epub
└── book6.epub
```

#### ❌ 错误结构（书籍直接放在根目录下，无法在前端显示）

```
books
├── book1.epub # ❌ 不会显示
├── book2.epub # ❌ 不会显示
└── SeriesA
└── book3.epub # ✅ 正确
```

> ⚠️ 注意：当前版本所有书籍都必须放在 **二级目录（系列）** 中，根目录下的书籍不会显示。未来会支持多级系列结构。
## 待实现功能

### 🚀 功能规划

- ⏱️ 阅读时长统计：自动记录并分析用户的阅读时长
- 📂 文件管理优化：支持本地文件阅读与上传，实现统一管理
- 🏷️ 标签管理功能：系列以及书籍标签
- 💾 存储方式支持：缓存文件支持 MinIO 对象存储与本地文件系统
- 🗂️ 系列结构支持：支持多级系列（一级、二级、三级等）显示，书籍根据目录层级自动归类

## 许可证

MIT
