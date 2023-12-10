import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;

public class StateObserver implements Control{
    private int pid;

    public StateObserver(String name) {
        pid = Configuration.getPid(name + ".protocol");
    }

    @Override
    public boolean execute() {
        NodeBase node0 = (NodeBase) Network.get(0).getProtocol(pid);
        if (node0.isTrainingCycle()) {
            double avg_acc = 0;
            double min_acc = Double.MAX_VALUE;
            double max_acc = 0;
            for (int i = 0; i < Network.size(); i++) {
                NodeBase node = (NodeBase) Network.get(i).getProtocol(pid);
                avg_acc += node.getTestAccuracy();
                min_acc = Math.min(min_acc, node.getTestAccuracy());
                max_acc = Math.max(max_acc, node.getTestAccuracy());
            }

            avg_acc /= Network.size();
            System.out.printf("Min/Max/Average accuracy: %.3f %.3f %.3f\n", min_acc, max_acc, avg_acc);

            return avg_acc >= 90;
        }
        
        return false;
    }
}
