import java.util.ArrayList;

import peersim.config.FastConfig;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * This class implements the AllReduce scheme for communicating weight within
 * the network.
 */
public class AllReduce extends NodeBase {

    public AllReduce(String str) {
        super(str);
    }

    @Override
    public void shareWeights(Node node, int protocolID) {
        if (node.getID() == 0) {
            // Wait for all models to be received.
            while (receivedModels.size() < Constants.NETWORK_SIZE - 1) {
                // block
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {}
            }

            // Calculate average model.
            while (!receivedModels.isEmpty()) {
                ArrayList<Float> nextModel = receivedModels.pop();
                for (int i = 0; i < modelWeights.size(); i++) {
                    modelWeights.set(i, modelWeights.get(i) + nextModel.get(i));
                }
            }
            for (int i = 0; i < modelWeights.size(); i++) {
                modelWeights.set(i, modelWeights.get(i) / Constants.NETWORK_SIZE);
            }

            // Distribute average model back to nodes.
            int linkableID = FastConfig.getLinkable(protocolID);
            Linkable linkable = (Linkable) node.getProtocol(linkableID);
            for (int i = 0; i < linkable.degree(); i++) {
                Node neighbor_node = linkable.getNeighbor(i);
                NodeBase nodeBase = (NodeBase) neighbor_node.getProtocol(protocolID);
                nodeBase.sendTo(modelWeights);
            }
        } else {
            // Send weights to node 0.
            int linkableID = FastConfig.getLinkable(protocolID);
            Linkable linkable = (Linkable) node.getProtocol(linkableID);
            for (int i = 0; i < linkable.degree(); i++) {
                Node neighbor = linkable.getNeighbor(i);
                if (neighbor.getID() == 0) {
                    NodeBase receiver = (NodeBase) neighbor.getProtocol(protocolID);
                    receiver.sendTo(modelWeights);
                }
            }

            // Receive averaged model.
            while (receivedModels.size() < 1) {
                // block
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {}
            }
            ArrayList<Float> newModel = receivedModels.pop();
            modelWeights = new ArrayList<>(newModel);
        }
    }

    @Override
    public Object clone() {
        return new AllReduce("AllReduce");
    }
}
