import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

public class TestNode implements CDProtocol {
    public static List<Float> temp_weights;

    List<Float> weights;
    double test_accuracy;
    double test_loss;

    public TestNode(String str) throws IOException {
        weights = new ArrayList<>();

        System.out.println("Initializing node with random weights");
        Process modelInit = new ProcessBuilder("python", "modules/init.py").start();
        var input = modelInit.getInputStream();
        weightsFromInputStream(input);
        if (temp_weights == null) {
            temp_weights = new ArrayList<>(weights.size());
        }
    }

    void weightsFromInputStream(InputStream input) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        ArrayList<Float> weights = new ArrayList<>();
        while ((bytesRead = input.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i += 4) {
                byte[] bytes = { (byte) buffer[i], (byte) buffer[i + 1], (byte) buffer[i + 2], (byte) buffer[i + 3] };
                float weight = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                weights.add(weight);
            }
        }

        this.weights = weights;
    }

    void train() {
        try {
            Process train = new ProcessBuilder("python", "modules/train.py").start();
            var output = train.getOutputStream();
            var input = train.getInputStream();
            for (int i = 0; i < weights.size(); i++) {
                byte[] bytes = ByteBuffer.allocate(4).putFloat(weights.get(i)).array();
                output.write(bytes);
            }
            weightsFromInputStream(input);
        } catch (Exception e) {
            System.err.println("Failed to run train.py.");
            e.printStackTrace();
        }
    }

    void test() {
        try {
            Process test = new ProcessBuilder("python", "modules/test.py").start();
            var output = test.getOutputStream();
            var input = new Scanner(test.getInputStream());
            for (int i = 0; i < weights.size(); i++) {
                byte[] bytes = ByteBuffer.allocate(4).putFloat(weights.get(i)).array();
                output.write(bytes);
            }
            test_accuracy = input.nextDouble();
            test_loss = input.nextDouble();
            input.close();
        } catch (Exception e) {
            System.err.println("Failed to run train.py.");
            e.printStackTrace();
        }
    }

    @Override
    public void nextCycle(Node node, int protocolID) {
        // First, run a training iteration.
        train();
        // Then, run a test iteration.
        test();

        // Then, average our new model with neighbors.
        int linkableID = FastConfig.getLinkable(protocolID);
        Linkable linkable = (Linkable) node.getProtocol(linkableID);
        if (linkable.degree() > 0) {
            Node peer = linkable.getNeighbor(CommonState.r.nextInt(linkable
                    .degree()));

            // Failure handling
            if (!peer.isUp())
                return;

            TestNode neighbor = (TestNode) peer.getProtocol(protocolID);
            List<Float> other_weights = neighbor.getWeights();
            for (int i = 0; i < weights.size(); i++) {
                temp_weights.set(i, (weights.get(i) + other_weights.get(i)) / 2);
            }
            neighbor.setWeights(new ArrayList<>(temp_weights));
            this.weights = new ArrayList<>(temp_weights);
        }
    }

    public List<Float> getWeights() {
        return weights;
    }

    public void setWeights(List<Float> weights) {
        this.weights = weights;
    }

    @Override
    public Object clone() {
        try {
            return new TestNode("");
        } catch (IOException e) {
            System.err.println("Failed to create new TestNode instance.");
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }
}
