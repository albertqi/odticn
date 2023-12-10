CP := lib/peersim-1.0.5.jar:lib/jep-2.3.0.jar:lib/djep-1.0.0.jar:bin

test:
	mkdir -p bin/
	javac -cp "$(CP)" -d bin/ src/TestNode.java
	javac -cp "$(CP):bin" -d bin/ src/TestRunner.java
	java -cp "$(CP):bin" TestRunner

all_reduce:
	mkdir -p bin/
	javac -cp "$(CP)" -d bin/ src/Constants.java
	javac -cp "$(CP)" -d bin/ src/NodeBase.java
	javac -cp "$(CP)" -d bin/ src/AllReduce.java
	javac -cp "$(CP)" -d bin/ src/StateObserver.java
	java -cp "$(CP):bin" peersim.Simulator config/all_reduce.config

gossip_learning:
	mkdir -p bin/
	javac -cp "$(CP)" -d bin/ src/Constants.java
	javac -cp "$(CP)" -d bin/ src/NodeBase.java
	javac -cp "$(CP)" -d bin/ src/GossipLearning.java
	javac -cp "$(CP)" -d bin/ src/StateObserver.java
	java -cp "$(CP):bin" peersim.Simulator config/gossip_learning.config
