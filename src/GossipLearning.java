import java.io.IOException;
import java.util.ArrayList;

import peersim.core.CommonState;
import peersim.config.FastConfig;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * This class implements the GossipLearning scheme for communicating weight within
 * the network.
 */
public class GossipLearning extends NodeBase {

    // List of previous latencies for each node.
    private double[] prevLatencies = new double[Constants.NETWORK_SIZE];
    
    public GossipLearning(String str) {
        super(str);
    }

    // Send weights to a random node, returning the index of that node.
    private int sendToRandomNode(ArrayList<Node> neighbors, int protocolID) {
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
    public void shareWeights(Node node, int protocolID) {
        // Aggregate all weights from `receivedModels`.
        int numReceivedModels = receivedModels.size();
        while (!receivedModels.isEmpty()) {
            ArrayList<Float> weights = receivedModels.poll();
            for (int i = 0; i < weights.size(); i++) {
                modelWeights.set(i, modelWeights.get(i) + weights.get(i));
            }
        }
        for (int i = 0; i < modelWeights.size(); i++) {
            modelWeights.set(i, modelWeights.get(i) / (numReceivedModels + 1));
        }

        // Perform gossip learning.
        int linkableID = FastConfig.getLinkable(protocolID);
        Linkable linkable = (Linkable) node.getProtocol(linkableID);
        ArrayList<Node> neighbors = new ArrayList<Node>();
        for (int i = 0; i < linkable.degree(); i++) {
            neighbors.add(linkable.getNeighbor(i));
        }
        int n = neighbors.size();
        for (int i = 0; i < n / 2; i++) {
            int neighborIndex = sendToRandomNode(neighbors, protocolID);
            neighbors.remove(neighborIndex);
        }

        // Proceed to training.
        setTrain();
    }

    @Override
    public Object clone() {
        return new GossipLearning("GossipLearning");
    }
}
