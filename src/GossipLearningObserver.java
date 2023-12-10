import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;

public class GossipLearningObserver implements Control{
    private int pid;

    public GossipLearningObserver(String name) {
        pid = Configuration.getPid(name + ".protocol");
    }

    @Override
    public boolean execute() {
        double avg_acc = 0;
        double min_acc = Double.MAX_VALUE;
        double max_acc = 0;
        for (int i = 0; i < Network.size(); i++) {
            GossipLearning node = (GossipLearning) Network.get(i).getProtocol(pid);
            avg_acc += node.getTestAccuracy();
            min_acc = Math.min(min_acc, node.getTestAccuracy());
            max_acc = Math.max(max_acc, node.getTestAccuracy());
        }

        avg_acc /= Network.size();
        System.out.printf("Min/Max/Average accuracy: %.3f %.3f %.3f\n", min_acc, max_acc, avg_acc);

        return false;
        // return max_acc >= 90;
    }
}