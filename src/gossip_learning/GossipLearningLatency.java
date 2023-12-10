import java.io.IOException;
import java.util.ArrayList;

import peersim.core.CommonState;
import peersim.config.FastConfig;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * This class implements the GossipLearningLatency scheme for communicating weight within
 * the network.
 */
public class GossipLearningLatency extends GossipLearningRandom {

    // List of previous latencies for each node.
    private double[] prevLatencies = new double[Constants.NETWORK_SIZE];
    
    public GossipLearningLatency(String str) {
        super(str);
    }

    // Send weights to a random node based on latency, returning the index of that node.
    @Override
    protected int sendToRandomNode(ArrayList<Node> neighbors, int protocolID) {
        ArrayList<Double> latenciesCum = new ArrayList<Double>();
        double totalLatency = 0.0;
        for (int i = 0; i < neighbors.size(); i++) {
            int neighborID = (int) neighbors.get(i).getID();
            totalLatency += prevLatencies[neighborID];
            latenciesCum.add(totalLatency);
        }
        double rand = CommonState.r.nextDouble() * totalLatency;
        for (int i = 0; i < latenciesCum.size(); i++) {
            if (rand <= latenciesCum.get(i)) {
                Node neighbor = neighbors.get(i);
                NodeBase neighborGossipLearning = (NodeBase) neighbor.getProtocol(protocolID);
                prevLatencies[(int) neighbor.getID()] = neighborGossipLearning.sendTo(modelWeights);
                return i;
            }
        }
        return -1;
    }

    @Override
    public Object clone() {
        return new GossipLearningLatency("GossipLearningLatency");
    }
}
