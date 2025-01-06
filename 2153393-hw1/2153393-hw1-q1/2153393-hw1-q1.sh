#!/bin/bash

# Function to check if a number is prime
is_prime() {
    local num=$1
    if ((num < 2)); then
        echo 0
        return
    fi
    for ((i = 2; i * i <= num; i++)); do
        if ((num % i == 0)); then
            echo 0
            return
        fi
    done
    echo 1
}

# Initialize sum
sum=0

# Loop through 1 to 100 and sum up the prime numbers
for ((n = 0; n <= 100; n++)); do
    if [[ $(is_prime $n) -eq 1 ]]; then
        sum=$((sum + n))
    fi
done

# Output the result to the log file
echo "1~100之间所有质数和为：$sum"
echo "1~100之间所有质数和为: $sum" > 2153393-hw1-q1.log

