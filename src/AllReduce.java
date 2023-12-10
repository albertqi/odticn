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
        
    }

    @Override
    public Object clone() {
        return new AllReduce("AllReduce");
    }
}
