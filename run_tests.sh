#!/bin/bash

echo "All Reduce"
make all_reduce

killall python3

echo "All Reduce Decreased 2"
make all_reduce_decreased

killall python3

echo "All Reduce Decreased 4"
make all_reduce_decreased4

killall python3

echo "All Reduce Decreased 8"
make all_reduce_decreased8

killall python3

echo "Gossip Learning Random"
make gossip_learning_random

killall python3

echo "Gossip Learning Latency"
make gossip_learning_latency
