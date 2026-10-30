#!/bin/sh
# 判题沙箱启动脚本。
#
# 为什么需要它：source/ 由宿主机 bind mount 进来（判题容器要按同一宿主机路径挂载），
# 宿主机上的 owner 与容器内 app(uid 1000) 不一致时 app 无法写入。
# 这里以 root 启动、确保目录存在且可写后，再用 su-exec 降权到 app 运行。
#
# docker.sock 的权限：宿主机 socket 通常是 root:root 0660。降权到 app 后直接访问会
# Permission denied（表现为“清理孤儿容器失败”+ 判题调度不可用）。因此这里读取 socket
# 的 gid 并让 app 以该 gid 运行——只放开这一个设备文件所需的组权限，不用 root 跑 JVM。
set -e

# 判题工作目录：必须与 JUDGE_SOURCE_DIR 及宿主机挂载目标完全一致。
# 宿主机 bind mount 的 owner 与容器内 app 不一致时 app 无法写入，
# 所以这里以 root 确保目录存在且可写，再降权运行。
SOURCE_DIR="${JUDGE_SOURCE_DIR:-/var/lib/wangoj/source}"
mkdir -p "$SOURCE_DIR"
chown -R app:app "$SOURCE_DIR" 2>/dev/null || true
chmod 755 "$SOURCE_DIR"

# 默认沿用 app 自己的 gid；能读到 docker.sock 时改用它的 gid
RUN_GID="$(id -g app)"
SOCK=/var/run/docker.sock
if [ -S "$SOCK" ]; then
  SOCK_GID="$(stat -c '%g' "$SOCK" 2>/dev/null || echo '')"
  if [ -n "$SOCK_GID" ] && [ "$SOCK_GID" != "0" ]; then
    # 非 root 组：建一个同名组再加进去，避免把 app 提升到 root 组
    if ! getent group "$SOCK_GID" >/dev/null 2>&1; then
      addgroup -g "$SOCK_GID" dockersock 2>/dev/null || true
    fi
    RUN_GID="$SOCK_GID"
    echo "judge: docker.sock gid=$SOCK_GID，以该组运行以便创建判题容器"
  elif [ -n "$SOCK_GID" ]; then
    # socket 属于 root 组：容器内加不了 gid 0，直接以 app:0 运行
    RUN_GID="0"
    echo "judge: docker.sock 属于 root 组，以 app:root 运行以便创建判题容器"
  fi
fi

exec su-exec app:"$RUN_GID" java $JAVA_OPTS -jar /opt/wangoj/judge.jar
