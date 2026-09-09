#!/usr/bin/env bash
# 域/模块: 平台底座/CI 防线
# 类型: 检查脚本
# 职责: 机械执行铁律 L1——backend 下每个 .java 文件必须含文件头注释（域/模块 + 设计文档行）
# 设计文档: docs/design/平台底座/design.md
# 维护者: 协调者 / agent-fffabc
# 用法: bash backend/scripts/check-file-headers.sh（在仓库任意位置执行均可）；违规输出清单并退出码 1
set -euo pipefail
cd "$(dirname "$0")/.."

fail=0
count=0
while IFS= read -r f; do
  count=$((count + 1))
  head_lines=$(head -n 15 "$f")
  echo "$head_lines" | grep -q "// 域/模块:" || { echo "缺 L1 头[// 域/模块:]: $f"; fail=1; }
  echo "$head_lines" | grep -q "// 设计文档:" || { echo "缺 L1 头[// 设计文档:]: $f"; fail=1; }
done < <(find . -name "*.java" -not -path "*/target/*")

if [ "$fail" -ne 0 ]; then
  echo "L1 文件头检查未通过（规则见 docs/architecture/iron-laws.md L1）"
  exit 1
fi
echo "L1 文件头检查通过：$count 个 .java 文件"
