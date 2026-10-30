#!/usr/bin/env bash
#
# 预拉取判题所需的语言镜像。
#
# 用法（在仓库根目录执行）：
#   ./judge/backend/install.sh          # 拉取全部镜像
#   ./judge/backend/install.sh cpp py   # 只拉取指定语言
#
# 注意：
#   - 镜像 tag 必须固定到具体版本，禁止使用 latest 或浮动 tag，否则不同环境判题结果可能不同。
#   - 修改镜像版本时，必须同步修改 judge/src/main/java/cn/wmhwiki/judge/pojo/lang/*.java 中的 image 字段。
#   - 本脚本在镜像已存在时会先检查，可重复执行。
#
set -euo pipefail

declare -A IMAGES=(
  [c]="gcc:13-bookworm"
  [cpp]="gcc:13-bookworm"
  [java]="eclipse-temurin:17-jdk-alpine"
  [python]="python:3.12-alpine"
  [js]="node:20-alpine"
  [go]="golang:1.22-alpine"
)

usage() {
  echo "用法: $0 [语言...]"
  echo "可用语言: ${!IMAGES[*]}"
}

if ! command -v docker >/dev/null 2>&1; then
  echo "错误：未找到 docker 命令，请先安装 Docker 并确保当前用户有权限访问。" >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "错误：无法连接 Docker 守护进程，请检查 Docker 是否启动。" >&2
  exit 1
fi

TARGETS=("$@")
if [ ${#TARGETS[@]} -eq 0 ]; then
  TARGETS=("${!IMAGES[@]}")
fi

PULLED=()
for lang in "${TARGETS[@]}"; do
  image="${IMAGES[$lang]:-}"
  if [ -z "$image" ]; then
    echo "警告：未知语言 '$lang'，已跳过。" >&2
    continue
  fi

  # 同一镜像可能被多种语言共用（例如 c 和 cpp），只拉一次
  if [[ " ${PULLED[*]} " == *" $image "* ]]; then
    continue
  fi

  if docker image inspect "$image" >/dev/null 2>&1; then
    echo "[跳过] $lang -> $image 已存在"
  else
    echo "[拉取] $lang -> $image"
    docker pull "$image"
  fi
  PULLED+=("$image")
done

echo
echo "完成。已就绪镜像："
for image in "${PULLED[@]}"; do
  docker image inspect --format '  {{.RepoTags}} {{.Id}}' "$image"
done
