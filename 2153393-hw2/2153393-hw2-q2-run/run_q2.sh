#!/bin/bash

# 切换到项目根目录
cd ..

# 设置CLASSPATH，包含当前目录和mypackage目录
export CLASSPATH=.:./mypackage

# 启动Server
gnome-terminal -- bash -c "java Class.Server; exec bash"

# 等待Server启动
sleep 3

# 启动Client1
gnome-terminal -- bash -c "java Class.Client1; exec bash"

# 启动Client2
gnome-terminal -- bash -c "java Class.Client2; exec bash"

# 启动Client3
gnome-terminal -- bash -c "java Class.Client3; exec bash"

