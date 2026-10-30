# judge —— 判题沙箱

本目录是 WangOJ 的判题沙箱服务：接收后端下发的代码与测试输入，在独立 Docker 容器中编译、执行，返回每个测试点的执行结果。

## 职责边界

- **沙箱只负责执行，不判断答案。** 输出的正确性比较在后端完成。
- 沙箱不面向终端用户，只接受后端的请求，并要求携带 `X-Judge-Token`。

## 运行前准备

1. 安装 Docker，并确认当前用户可访问 Docker 守护进程：

   ```bash
   docker info
   ```

2. 预拉取语言镜像（tag 已固定）：

   ```bash
   ./backend/install.sh          # 全部
   ./backend/install.sh cpp java # 只拉取部分
   ```

3. 配置共享密钥（必须与后端一致，否则所有请求会被拒绝）：

   ```bash
   export JUDGE_TOKEN="$(openssl rand -hex 32)"
   export JUDGE_PORT=8122
   ```

## 启动

沙箱是独立 Maven 工程，构建时用 `-f` 指定沙箱的 pom。请在**仓库根目录**执行：

```bash
mvn -f judge/pom.xml -DskipTests package
java -jar judge/target/judge.jar
```

开发时也可直接运行：

```bash
mvn -f judge/pom.xml spring-boot:run
```

## 接口

| 方法 | 路径 | 鉴权 |
| --- | --- | --- |
| GET | `/ping` | 否 |
| POST | `/api/v1/judge` | 是（`X-Judge-Token`） |

## 安全模型

- 判题容器：禁用网络、限制内存与 swap、限制 PID 数、`cap-drop ALL`、`no-new-privileges`、
  限制文件大小/句柄数，只挂载本次判题目录。
- 超时后主动 kill 进程，结束后强制删除容器。
- 沙箱端口**不要**暴露到公网或宿主机；只允许后端访问。

## 支持的镜像

| 语言 | 镜像 |
| --- | --- |
| c / c++ | `gcc:13-bookworm` |
| java | `eclipse-temurin:17-jdk-alpine` |
| python | `python:3.12-alpine` |
| javascript | `node:20-alpine` |
| golang | `golang:1.22-alpine` |

修改镜像版本时，必须同时修改 `src/main/java/cn/wmhwiki/judge/pojo/lang/*.java` 与 `backend/install.sh`。

## 来源与许可证

本目录基于上游开源工程（Apache License 2.0）修改而来。业务包名已由上游迁移为 `cn.wmhwiki.judge`，
Maven `groupId` 同步改为 `cn.wmhwiki`；上游归属声明（含原始 `groupId`）见本目录 [`NOTICE`](NOTICE)。
许可证全文见 [`LICENSE`](../LICENSE)。

