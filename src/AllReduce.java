import java.util.ArrayList;

import peersim.config.FastConfig;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * This class implements the AllReduce scheme for communicating weight within
 * the network.
 */
public class AllReduce extends NodeBase {
    
    private enum STATE {
        SEND,
        WAIT,
        RECEIVE
    }

    private STATE share_state = STATE.SEND;

    public AllReduce(String str) {
        super(str);
    }

    @Override
    public void shareWeights(Node node, int protocolID) {
        if (share_state == STATE.SEND) {
            share_state = STATE.WAIT;
            if (node.getID() == 0) {
                return;
            }
            
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
        } else if (share_state == STATE.WAIT) {
            share_state = STATE.RECEIVE;
            if (node.getID() != 0) {
                return;
            }
            // Calculate average model.
            ArrayList<Float> sumWeights = new ArrayList<>(modelWeights);
            while (!receivedModels.isEmpty()) {
                ArrayList<Float> nextModel = receivedModels.pop();
                for (int i = 0; i < sumWeights.size(); i++) {
                    sumWeights.set(i, sumWeights.get(i) + nextModel.get(i));
                }
            }
            for (int i = 0; i < sumWeights.size(); i++) {
                modelWeights.set(i, sumWeights.get(i) / Constants.NETWORK_SIZE);
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
            share_state = STATE.SEND;
            setTrain();
            if (node.getID() == 0) {
                return;
            }
            // Receive averaged model.
            ArrayList<Float> newModel = receivedModels.pop();
            modelWeights = new ArrayList<>(newModel);
        }
    }

    @Override
    public Object clone() {
        return new AllReduce("AllReduce");
    }
}
