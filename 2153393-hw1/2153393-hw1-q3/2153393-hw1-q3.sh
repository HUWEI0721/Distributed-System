#!/bin/bash

# 获取传入的日志文件名作为参数
log_file="$1"
output_file="2153393-hw1-q3.log"

# a) 计算日志文件的总行数
total_lines=$(wc -l < "$log_file")

# b) 计算日志文件的总字符数
total_chars=$(wc -m < "$log_file")

# c) 计算第一行和最后一行时间戳的时间差
first_timestamp=$(head -n 1 "$log_file" | awk '{print $1}')
last_timestamp=$(tail -n 1 "$log_file" | awk '{print $1}')
time_diff=$(( $(date -d "$last_timestamp" +%s) - $(date -d "$first_timestamp" +%s) ))

# d) 计算最后三列的平均值（假设这三列分别是第9,10,11列）
average_load=$(awk '{sum1+=$9; sum2+=$10; sum3+=$11} END {print sum1/NR, sum2/NR, sum3/NR}' "$log_file") 

# 将结果输出到指定的日志文件
echo "总行数: $total_lines" > "$output_file"
echo "总字符数: $total_chars" >> "$output_file"
echo "时间差: $time_diff 秒" >> "$output_file"
echo "系统负载平均值: $average_load" >> "$output_file"

# 输出结果到屏幕
cat "$output_file"

