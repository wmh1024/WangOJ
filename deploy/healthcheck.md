# 健康检查设计说明

三个运行时镜像都使用镜像内已有的工具做健康检查，不额外安装 `curl`：

| 服务 | 探测方式 | 判定 |
| --- | --- | --- |
| backend | busybox `sh` 的 `/dev/tcp` 请求 `/actuator/health` | 响应中包含 `"status":"UP"` |
| judge | busybox `sh` 的 `/dev/tcp` 请求 `/ping` | 响应中包含 `success` |
| web | nginx 镜像自带的 `wget` 请求 `/healthz` | 响应为 `ok` |

## 为什么不用 `curl`

后端与 judge 运行在 `eclipse-temurin:17-jre-alpine` 上，默认没有 `curl`/`wget`。
安装 `curl` 会增加约 5MB 镜像体积；`/dev/tcp` 是 busybox `sh` 的内置能力，足够探测本机端口。

## 截断长度

早期实现只读取 200 字节，Actuator 响应字段顺序变化时可能截断在 `"status":"UP"` 之前。
现在 backend 读取 4096 字节、judge 读取 1024 字节，并只匹配稳定子串，避免误判。

## 局限

- 这是**存活探测**，不是业务级探针；进程能响应 HTTP 即视为健康。
- 数据库/Redis 不可用时 Actuator 可能显示 `DOWN`，此时 compose 会重启 backend；
  若你希望区分“进程活着但依赖故障”，请把健康检查改为只请求 `/actuator/health/liveness`。
