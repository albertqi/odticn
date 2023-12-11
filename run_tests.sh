#!/bin/bash

echo "All Reduce"
make all_reduce | tee all_reduce.txt

killall python3

echo "All Reduce Decreased 2"
make all_reduce_decreased | tee all_reduce_decreased_2.txt

killall python3

echo "All Reduce Decreased 4"
make all_reduce_decreased4 | tee all_reduce_decreased_4.txt

killall python3

echo "All Reduce Decreased 8"
make all_reduce_decreased8 | tee all_reduce_decreased_8.txt

killall python3

echo "Gossip Learning Random"
make gossip_learning_random | tee gossip_learning_random.txt

killall python3

echo "Gossip Learning Latency"
make gossip_learning_latency | tee gossip_learning_latency.txt

