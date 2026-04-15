# 精灵王国（养成回合制游戏）

一个基于 `Java 17 + Spring Boot + Thymeleaf + JPA + MySQL` 开发的单机版精灵养成对战游戏。项目整体玩法接近经典的宠物收集与回合制战斗模式，支持本地存档、野外遭遇、训练师对战、图鉴收集、背包与仓库管理、孵蛋养成等功能。

当前项目采用“单机本地存档”模式，启动后无需注册或登录，直接进入游戏主菜单即可开始游玩。

## 项目特点

- 单机模式：根路径会直接跳转到 `/game`，不依赖账号登录流程
- 本地存档：游戏数据持久化到 MySQL，关闭项目后仍可继续读取进度
- 回合制战斗：支持野外战斗、训练师战斗、部分副本挑战
- 精灵养成：包含等级、HP、MP、攻击、防御、速度、性别、稀有度等属性
- 图鉴系统：记录遇见、捕获、抽奖获得等统计信息
- 背包与仓库：支持容量限制、仓库扩容、精灵转移、奖励自动分配
- 孵蛋系统：支持符合规则的精灵配对、孵化进度推进与子代生成
- 商店与治疗：支持购买精灵球、恢复道具，以及医院治疗
- 成就系统：支持图鉴和金币类成就解锁与奖励领取

## 技术栈

- Java 17
- Spring Boot 4.0.4
- Spring MVC
- Spring Data JPA
- Thymeleaf
- MySQL 8
- Maven Wrapper

## 项目结构

项目根目录主要包含以下内容：

```text
.
├─ README.md
├─ start-game.bat          # 根目录一键启动脚本
└─ demo/                   # Spring Boot 主项目
   ├─ pom.xml
   ├─ mvnw.cmd
   └─ src/
      ├─ main/java/com/example/demo/pokemon/
      │  ├─ config/
      │  ├─ controller/
      │  ├─ entity/
      │  ├─ enums/
      │  ├─ repository/
      │  └─ service/
      ├─ main/resources/
      │  ├─ application.properties
      │  ├─ templates/
      │  └─ static/
      └─ test/
```

## 核心功能

### 1. 主菜单与存档

- 访问 `/game` 进入主菜单
- 支持“新游戏”“读取存档”“关于游戏”
- 新游戏会重置当前本地玩家数据
- 读取存档会继续使用数据库中已有的游戏进度

### 2. 精灵系统

- 支持火、水、草、普通等属性
- 支持稀有度、等级、技能、性别等信息展示
- 精灵可捕获、出售、治疗、入仓、出仓、切换出战
- 精灵至少可拥有基础技能，战斗内可消耗 MP 释放技能

### 3. 战斗系统

- 野外战斗
- 训练师 `6v6` 对战
- 战斗中攻击、换宠、使用道具、捕获、逃跑
- 战斗结果会结算金币、经验和部分养成进度

### 4. 背包与仓库

- 背包容量上限为 `6`
- 仓库初始容量为 `30`
- 可使用金币扩容仓库
- 奖励精灵会优先放入背包，背包装满后进入仓库
- 若背包和仓库都已满，部分奖励会自动折算为金币

### 5. 孵蛋系统

- 支持符合规则的精灵进行配对
- 孵蛋需要消耗金币
- 最多可同时孵化多个蛋
- 战斗会推进孵蛋进度
- 孵化完成后返还父母并生成子代精灵

### 6. 图鉴、商店、医院、成就

- 图鉴记录遇见、捕获和抽奖次数
- 商店可购买精灵球与恢复类道具
- 医院可治疗背包中的精灵
- 成就系统支持进度展示和奖励领取

## 环境要求

- JDK 17 或更高版本
- MySQL 8.x
- Maven 3.6+，或直接使用项目自带的 Maven Wrapper

## 数据库配置

默认配置位于 [application.properties](./demo/src/main/resources/application.properties)：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/pokemon_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
```

默认情况下：

- 数据库名：`pokemon_db`
- 用户名：`root`
- 密码：`root`
- JPA 自动建表策略：`update`
- 服务端口：`8080`

如果你的 MySQL 用户名或密码不同，请先修改 `demo/src/main/resources/application.properties` 后再启动。

## 启动方式

### 方式一：双击脚本启动

项目根目录提供了 [start-game.bat](./start-game.bat)：

```bat
start-game.bat
```

脚本会自动：

- 进入 `demo/` 目录
- 执行 `mvnw.cmd spring-boot:run`
- 等待服务启动
- 自动打开浏览器访问 `http://localhost:8080/game`

### 方式二：命令行启动

在 `demo/` 目录下执行：

```bash
./mvnw spring-boot:run
```

Windows 下也可以使用：

```bat
mvnw.cmd spring-boot:run
```

如果本机已安装 Maven，也可以执行：

```bash
mvn spring-boot:run
```

## 访问地址

- 首页入口：`http://localhost:8080/`
- 游戏主界面：`http://localhost:8080/game`
- 野外地图：`http://localhost:8080/wild-map`
- 野外战斗页：`http://localhost:8080/wild-battle`

说明：

- 访问 `/` 会自动重定向到 `/game`
- 当前为单机模式，无需登录

## 主要接口与页面入口

后端核心入口主要集中在以下控制器中：

- [AuthController.java](./demo/src/main/java/com/example/demo/pokemon/controller/AuthController.java)：单机模式入口与兼容接口
- [GameController.java](./demo/src/main/java/com/example/demo/pokemon/controller/GameController.java)：游戏主流程、战斗、背包、图鉴、存档、孵蛋等
- [PokemonController.java](./demo/src/main/java/com/example/demo/pokemon/controller/PokemonController.java)：精灵基础 REST 接口

前端页面模板位于：

- [game.html](./demo/src/main/resources/templates/game.html)
- [wild-map.html](./demo/src/main/resources/templates/wild-map.html)
- [wild-battle.html](./demo/src/main/resources/templates/wild-battle.html)

## 静态资源

静态资源位于 `demo/src/main/resources/static/`：

- `images/pokemon/`：精灵图片资源
- `images/menu/`：菜单背景图
- `audio/`：背景音乐与战斗音乐

## 注意事项

- 首次运行前请确认 MySQL 服务已启动
- 项目默认会连接本地 `3306` 端口的 MySQL
- 由于使用 JPA `update` 策略，首次启动会自动创建或更新表结构
- 项目中包含一些运行日志文件和临时文件，不影响主程序使用
- 若浏览器未自动打开，可手动访问 `http://localhost:8080/game`

## 后续可优化方向

- 补充更多自动化测试
- 优化前端页面样式与交互反馈
- 继续扩展精灵、技能、地图与副本内容
- 增强数值平衡与战斗策略深度
- 清理历史兼容代码与临时日志文件

## 作者说明

根据项目内现有说明文档，“关于游戏”中展示的开发者信息为：`李涌`。

---

如果你是第一次接手这个项目，推荐的最短启动路径是：

1. 启动本地 MySQL
2. 检查 `demo/src/main/resources/application.properties` 中的数据库账号密码
3. 双击根目录 `start-game.bat`
4. 浏览器打开 `http://localhost:8080/game` 后开始游戏
