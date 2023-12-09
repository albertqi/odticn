import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

public class TestNode implements CDProtocol {
    public static final int NETWORK_SIZE = 3;

    public static Float[] temp_weights;
    public static int sum_count = 0;

    boolean train_cycle = true;

    List<Float> weights;
    double test_accuracy;
    double test_loss;

    int cycle_count = 0;

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

        this.weights = weights;
    }

    public void train(int id) {
        try {
            Process train = new ProcessBuilder("python3", "modules/train.py", "" + weights.size(), Integer.toString(NETWORK_SIZE), "" + id).start();
            var output = train.getOutputStream();
            var input = train.getInputStream();
            var error = train.getErrorStream();
            new Thread(() -> {
                Scanner scanner = new Scanner(error);
                while (scanner.hasNext()) {
                    System.err.println(scanner.nextLine());
                }
                scanner.close();
            }).start();
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
            Process test = new ProcessBuilder("python3", "modules/test.py", "" + weights.size(), Integer.toString(NETWORK_SIZE), "" + id).start();
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
            System.err.println("Failed to run test.py.");
            e.printStackTrace();
        }
    }

    void shareWeights(Node node, int protocolID) {
        // Then, average our new model with neighbors.
        int linkableID = FastConfig.getLinkable(protocolID);
        Linkable linkable = (Linkable) node.getProtocol(linkableID);
        if (linkable.degree() > 0) {
            // Node peer = linkable.getNeighbor(CommonState.r.nextInt(linkable
            //         .degree()));

            // // Failure handling
            // if (!peer.isUp())
            //     return;

            // TestNode neighbor = (TestNode) peer.getProtocol(protocolID);
            // List<Float> other_weights = neighbor.getWeights();
            for (int i = 0; i < temp_weights.length; i++) {
                if (sum_count == 0) {
                    temp_weights[i] = 0.0f;
                }
                temp_weights[i] += weights.get(i);
                if (sum_count == 3) {
                    temp_weights[i] /= 3;
                }
            }
            if (++sum_count == 3) {
                sum_count = 0;
                this.weights = new ArrayList<>(Arrays.asList(temp_weights));
                for (int i = 0; i < linkable.degree(); i++) {
                    Node neighbor_node = linkable.getNeighbor(i);
                    TestNode neighbor_test_node = (TestNode) neighbor_node.getProtocol(protocolID);
                    neighbor_test_node.setWeights(new ArrayList<>(Arrays.asList(temp_weights)));
                }
            }
        }
    }

    @Override
    public void nextCycle(Node node, int protocolID) {
        if (train_cycle) {
            // First, run a training iteration.
            train(node.getIndex());
            // Then, run a test iteration.
            test(node.getIndex());
        } else {
            shareWeights(node, protocolID);
            test(node.getIndex());
        }
        
        train_cycle = !train_cycle;
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
