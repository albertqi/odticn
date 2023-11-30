import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;

public class TestNode implements CDProtocol {
    public static Float[] temp_weights;

    List<Float> weights;
    double test_accuracy;
    double test_loss;

    public TestNode(String str) throws IOException {
        weights = new ArrayList<>();

        System.out.println("Initializing node with random weights");
        Process modelInit = new ProcessBuilder("python3", "modules/init.py").start();
        var input = modelInit.getInputStream();
        weightsFromInputStream(input);
        if (temp_weights == null) {
            temp_weights = new Float[weights.size()];
        }
    }

    void weightsFromInputStream(InputStream input) throws IOException {
        ArrayList<Float> weights = new ArrayList<>();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i += 4) {
                byte[] bytes = { (byte) buffer[i], (byte) buffer[i + 1], (byte) buffer[i + 2], (byte) buffer[i + 3] };
                float weight = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                weights.add(weight);
            }
        }
        System.out.println("num weights: " + weights.size());

        this.weights = weights;
    }

    public void train(int id) {
        try {
            Process train = new ProcessBuilder("python3", "modules/train.py", "" + weights.size(), "" + 3, "" + id).start();
            var output = train.getOutputStream();
            var input = train.getInputStream();
            for (int i = 0; i < weights.size(); i++) {
                byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(weights.get(i)).array();
                output.write(bytes);
            }
            output.flush();
            weightsFromInputStream(input);
        } catch (Exception e) {
            System.err.println("Failed to run train.py.");
            e.printStackTrace();
        }
    }
  
    public void test(int id) {
        try {
            Process test = new ProcessBuilder("python3", "modules/test.py", "" + weights.size(), "" + 3, "" + id).start();
            var output = test.getOutputStream();
            var input = new Scanner(test.getInputStream());
            for (int i = 0; i < weights.size(); i++) {
                byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(weights.get(i)).array();
                output.write(bytes);
            }
            output.flush();
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
        train(node.getIndex());
        // Then, run a test iteration.
        test(node.getIndex());

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
                temp_weights[i] = (weights.get(i) + other_weights.get(i)) / 2.0f;
            }
            neighbor.setWeights(new ArrayList<>(Arrays.asList(temp_weights)));
            this.weights = new ArrayList<>(Arrays.asList(temp_weights));
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
