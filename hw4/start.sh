#!/bin/bash

# 设置目标目录
TARGET_DIR="Server/bin"

# 检查是否安装了 tmux
if ! command -v tmux &> /dev/null
then
    echo "tmux 未安装。请先安装 tmux。"
    exit 1
fi

# 检查目标目录是否存在
if [ ! -d "$TARGET_DIR" ]; then
    echo "目标目录 $TARGET_DIR 不存在。请确保路径正确。"
    exit 1
fi

# 切换到目标目录
cd "$TARGET_DIR" || { echo "无法切换到目录 $TARGET_DIR"; exit 1; }

# 检查是否已编译 ServerMain
if [ ! -f "ServerMain.class" ]; then
    echo "未找到 ServerMain.class。请先编译 ServerMain.java。"
    exit 1
fi

# 检查 protobuf-java-3.21.10.jar 是否存在
PROTOBUF_JAR="protobuf-java-3.21.10.jar"
if [ ! -f "$PROTOBUF_JAR" ]; then
    echo "未找到 $PROTOBUF_JAR。请确保该文件存在于 $TARGET_DIR 目录中。"
    exit 1
fi

# 创建输入文件目录（在目标目录内）
INPUT_DIR="./inputs"
mkdir -p "$INPUT_DIR"

# 为第一个虚拟机创建输入文件 (虚拟机0)
cat <<EOL > "$INPUT_DIR/vm0_input.txt"
0
0
yes
12000
EOL

# 为第二个虚拟机创建输入文件 (虚拟机1)
cat <<EOL > "$INPUT_DIR/vm1_input.txt"
1
1
no
10000
EOL

# 启动第一个 tmux 会话 (vm0)
tmux new-session -d -s vm0 "java -cp \".:$PROTOBUF_JAR\" ServerMain < $INPUT_DIR/vm0_input.txt"

# 启动第二个 tmux 会话 (vm1)
tmux new-session -d -s vm1 "java -cp \".:$PROTOBUF_JAR\" ServerMain < $INPUT_DIR/vm1_input.txt"

echo "两个虚拟机已启动在独立的 tmux 会话中。"

# 可选：显示当前 tmux 会话列表
tmux ls

# 可选：附加到第一个 tmux 会话以查看输出
# 取消注释下一行以自动附加
# tmux attach-session -t vm0