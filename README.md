# AI Agent 旅行规划助手

一个基于 Spring Boot 和 Spring AI 开发的智能旅行规划助手，集成高德地图 API 提供地理信息服务。

## 功能特性

### 核心功能
- **智能旅行规划**：根据用户需求生成个性化旅行方案
- **地理信息服务**：集成高德地图 API 提供丰富的地理数据
- **工具扩展机制**：支持多种工具的集成和扩展

### 集成工具
- **文件操作工具**：提供文件读写操作能力
- **PDF 生成工具**：支持生成旅行规划 PDF 文档
- **网页抓取工具**：获取互联网上的旅行相关信息
- **终端操作工具**：执行系统命令
- **高德地图 API 工具**：
  - 地理编码：将地址转换为经纬度坐标
  - 逆地理编码：将经纬度转换为地址信息
  - 驾车路径规划：获取最优驾车路线
  - 步行路径规划：获取步行路线
  - 兴趣点搜索：搜索美食、景点等旅行相关地点

## 技术栈

- **后端框架**：Spring Boot 3.4.12
- **编程语言**：Java 21
- **AI 框架**：Spring AI 1.0.0
- **大模型**：阿里巴巴达摩院 DashScope
- **地理服务**：高德地图 API
- **工具库**：
  - Hutool：Java 工具集
  - iText：PDF 生成
  - Jsoup：HTML 解析
  - Lombok：简化 Java 代码
  - MyBatis：ORM 框架
- **API 文档**：Knife4j (OpenAPI 3)
- **数据库**：MySQL

## 快速开始

### 环境要求
- JDK 21+
- Maven 3.6+
- MySQL 8.0+

### 安装步骤

1. **克隆仓库**
   ```bash
   git clone https://github.com/your-username/ai-agent.git
   cd ai-agent
   ```

2. **编译构建**
   ```bash
   mvn clean install
   ```

3. **数据库配置**
   - 创建数据库：`ai_agent_db`
   - 导入数据库表结构（如有需要）

4. **配置 API 密钥**
   编辑 `src/main/resources/application.yml` 文件，配置必要的 API 密钥：
   ```yaml
   spring:
     ai:
       dashscope:
         api-key: ${ai.api-key}  # 阿里巴巴达摩院 API 密钥
   
   # 高德地图 API 配置
   amap:
     api-key: ${amap.api-key}  # 高德地图 API 密钥
   ```

5. **启动应用**
   ```bash
   mvn spring-boot:run
   ```

6. **访问应用**
   - API 文档：http://localhost:8123/api/swagger-ui.html
   - 应用接口：http://localhost:8123/api

## 使用示例

### 1. 地理编码
将地址转换为经纬度坐标：

```java
AmapAPITool amapTool = new AmapAPITool("your-amap-api-key");
String result = amapTool.geocode("南京市", null);
System.out.println(result);
```

### 2. 兴趣点搜索
搜索镇江的美食地点：

```java
String foodPlaces = amapTool.placeSearch("美食", "镇江市", "100000", 10);
System.out.println(foodPlaces);
```

### 3. 路径规划
计算南京到镇江的驾车路线：

```java
String drivingRoute = amapTool.drivingDirection(
    "118.796877,32.060255",  // 南京经纬度
    "119.449011,32.204094",  // 镇江经纬度
    null
);
System.out.println(drivingRoute);
```

### 4. 步行路径规划
计算镇江市区内的步行路线：

```java
String walkingRoute = amapTool.walkingDirection(
    "119.443818,32.206882",  // 镇江火车站经纬度
    "119.449011,32.204094"   // 镇江美食点经纬度
);
System.out.println(walkingRoute);
```

## 项目结构

```
src/
├── main/
│   ├── java/com/zhishi/aiagent/
│   │   ├── controller/        # 控制器层
│   │   ├── dto/               # 数据传输对象
│   │   ├── mapper/            # MyBatis 映射器
│   │   ├── service/           # 业务逻辑层
│   │   ├── tools/             # 工具类
│   │   │   ├── AmapAPITool.java      # 高德地图 API 工具
│   │   │   ├── FileOperationTool.java # 文件操作工具
│   │   │   ├── PDFGenerationTool.java # PDF 生成工具
│   │   │   └── ...
│   │   └── Application.java   # 应用主类
│   └── resources/
│       ├── mapper/            # MyBatis 映射文件
│       ├── application.yml    # 应用配置
│       └── mcp-servers.json   # MCP 服务器配置
└── test/                      # 测试代码
```

## API 文档

项目使用 Knife4j 生成 OpenAPI 3 标准的 API 文档，可通过以下地址访问：

http://localhost:8123/api/swagger-ui.html

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 联系方式

- 项目地址：https://github.com/your-username/ai-agent
- 问题反馈：https://github.com/your-username/ai-agent/issues

## 更新日志

### v0.0.1-SNAPSHOT (2025-12-24)
- 初始版本发布
- 集成基础工具集
- 新增高德地图 API 工具，支持地理编码、路径规划、兴趣点搜索等功能