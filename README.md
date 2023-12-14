# Optimizing Distributed Training on Intermittently Connected Networks
## Albert Qi, Ronak Malik, and Steve Dalla
### Fall 2023

As machine learning workloads and dataset sizes continue to grow, machine learning training will inevitably become more distributed. Whether this is through techniques such as highly distributed training on edge devices or just the expansion to multiple datacenters, proper optimization of these distributed systems will become increasingly critical. However, this optimization needs to be applicable for different scenarios, including ones with slow or unstable networks. It is not reasonable to assume that distributed training will only ever be run across good, stable connections, and as such, it is increasingly important that strategies for optimizing distributed training on intermittently connected networks are developed.

There are a few strategies that we test. We first experiment with decreasing the frequency of weight aggregation of an all-reduce algorithm, seeing how it affects accuracy and performance on a simple triangle network. We also experiment with gossip learning, with both random neighbor communication and network-aware communication, and compare it to our previous strategies. Then, we test all of these strategies on more complex networks with a higher number of nodes. On a simple triangle network, performing all-reduce every four iterations results in the fastest time-to-accuracy, ultimately converging 53.8% faster than standard all-reduce and 43.7% faster than the best gossip learning strategy. On a network consisting of nine nodes, performing all-reduce every two iterations results in the fastest time-to-accuracy, ultimately converging 28.3% faster than standard all-reduce while gossip learning fails to converge.
