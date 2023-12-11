CP := lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:bin

all_reduce:
	mkdir -p bin/
	javac -cp "$(CP)" -d bin/ src/Constants.java
	javac -cp "$(CP)" -d bin/ src/NodeBase.java
	javac -cp "$(CP)" -d bin/ src/all_reduce/AllReduce.java
	java -cp "$(CP):bin" peersim.Simulator config/all_reduce/all_reduce.config

all_reduce_decreased:
	mkdir -p bin/
	javac -cp "$(CP)" -d bin/ src/Constants.java
	javac -cp "$(CP)" -d bin/ src/NodeBase.java
	javac -cp "$(CP)" -d bin/ src/all_reduce/AllReduce.java
	javac -cp "$(CP)" -d bin/ src/all_reduce/AllReduceDecreased.java
	java -cp "$(CP):bin" peersim.Simulator config/all_reduce/all_reduce_decreased.config

all_reduce_decreased4:
	mkdir -p bin/
	javac -cp "$(CP)" -d bin/ src/Constants.java
	javac -cp "$(CP)" -d bin/ src/NodeBase.java
	javac -cp "$(CP)" -d bin/ src/all_reduce/AllReduce.java
	javac -cp "$(CP)" -d bin/ src/all_reduce/AllReduceDecreased.java
	java -cp "$(CP):bin" peersim.Simulator config/all_reduce/all_reduce_decreased4.config

all_reduce_decreased8:
	mkdir -p bin/
	javac -cp "$(CP)" -d bin/ src/Constants.java
	javac -cp "$(CP)" -d bin/ src/NodeBase.java
	javac -cp "$(CP)" -d bin/ src/all_reduce/AllReduce.java
	javac -cp "$(CP)" -d bin/ src/all_reduce/AllReduceDecreased.java
	java -cp "$(CP):bin" peersim.Simulator config/all_reduce/all_reduce_decreased8.config

gossip_learning_random:
	mkdir -p bin/
	javac -cp "$(CP)" -d bin/ src/Constants.java
	javac -cp "$(CP)" -d bin/ src/NodeBase.java
	javac -cp "$(CP)" -d bin/ src/gossip_learning/GossipLearningRandom.java
	java -cp "$(CP):bin" peersim.Simulator config/gossip_learning/gossip_learning_random.config

gossip_learning_latency:
	mkdir -p bin/
	javac -cp "$(CP)" -d bin/ src/Constants.java
	javac -cp "$(CP)" -d bin/ src/NodeBase.java
	javac -cp "$(CP)" -d bin/ src/gossip_learning/GossipLearningRandom.java
	javac -cp "$(CP)" -d bin/ src/gossip_learning/GossipLearningLatency.java
	java -cp "$(CP):bin" peersim.Simulator config/gossip_learning/gossip_learning_latency.config
