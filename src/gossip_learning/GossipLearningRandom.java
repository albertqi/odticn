import java.io.IOException;
import java.util.ArrayList;

import peersim.core.CommonState;
import peersim.config.FastConfig;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * This class implements the GossipLearningRandom scheme for communicating weight within
 * the network.
 */
public class GossipLearningRandom extends NodeBase {

    public GossipLearningRandom(String str) {
        super(str);
    }

    // Send weights to a random node, returning the index of that node.
    protected int sendToRandomNode(Node node, ArrayList<Node> neighbors, int protocolID) {
        int randIndex = CommonState.r.nextInt(neighbors.size());
        Node neighbor = neighbors.get(randIndex);
        NodeBase neighborGossipLearning = (NodeBase) neighbor.getProtocol(protocolID);
        neighborGossipLearning.sendTo((int) node.getID(), modelWeights);
        return randIndex;
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
            int neighborIndex = sendToRandomNode(node, neighbors, protocolID);
            neighbors.remove(neighborIndex);
        }
    }

    @Override
    public Object clone() {
        return new GossipLearningRandom("GossipLearningRandom");
    }
}
