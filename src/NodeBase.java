import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import peersim.cdsim.CDProtocol;
import peersim.core.CommonState;
import peersim.core.Node;

/**
 * Base class that implements basic distribued learning functionality. Protocols
 * and strategies should extend this class and implement the shareWeights()
 * method.
 */
public abstract class NodeBase implements CDProtocol {
    
    /**
     * Paths to PyTorch scripts from the project root.
     */
    private static final String initScriptPath = "modules/init.py";
    private static final String trainScriptPath = "modules/train.py";
    private static final String testScriptPath = "modules/test.py";

    /**
     * The weights for the current model iteration.
     */
    protected ArrayList<Float> modelWeights;

    /**
     * The models that this node has reveiced from peers since the last cycle.
     */
    protected ArrayDeque<ArrayList<Float>> receivedModels;

    /**
     * The test accuracy and loss calucated after each training cycle.
     */
    private double testAccuracy;
    private double testLoss;

    /**
     * Tracks whether to train this cycle or share weights.
     */
    private boolean trainCycle;

    /**
     * Tracks how many training cycles have been completed.
     */
    private int currentIteration;

    /**
     * Tracks the accumlated latency in sending weights to this node.
     */
    private double receiveLatency;

    /**
     * List of current latencies from this node to every other node.
     */
    private double[] currLatencies;

    /**
     * Shares this model's weights throughout the network.
     * 
     * This method should be implemented by child classes.
     */
    public abstract void shareWeights(Node node, int protocolID);

    @Override
    public abstract Object clone();

    public NodeBase(String name) {
        currentIteration = 0;
        trainCycle = true;
        modelWeights = new ArrayList<>();
        receivedModels = new ArrayDeque<>();

        // Initialize latencies randomly from 1.5 to 2.5 seconds.
        currLatencies = new double[Constants.NETWORK_SIZE];
        for (int i = 0; i < currLatencies.length; i++) {
            currLatencies[i] = 1.5 + CommonState.r.nextDouble();
        }

        try {
            System.out.println("Initializing model...");
            InputStream input = runScript(initScriptPath, 0, false);
            modelWeights = parseWeights(input);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize weights");
        }
    }

    /**
     * Alternates between training and running the share weights protocol deined
     * by child classes. When a protocol is ready to go back to training, setTrain()
     * should be called. If training should be performed every other cycle,
     * children should call setTrain() every time shareWeights() is called.
     */
    @Override
    public void nextCycle(Node node, int protocolID) {
        if (trainCycle) {
            train(node.getIndex());
            test(node.getIndex());
            currentIteration++;
            trainCycle = false;
        } else {
            shareWeights(node, protocolID);
        }
    }
    
    /**
     * Sets the next cycle to be a training iteration.
     */
    protected void setTrain() {
        trainCycle = true;
    }

    /**
     * Returns whether the current cycle is a training cycle.
     */
    public boolean isTrainingCycle() {
        return trainCycle;
    }

    /**
     * @return The current test accuracy after the last training iteration.
     */
    public double getTestAccuracy() {
        return testAccuracy;
    }

    /**
     * Receives the given weights and adds them to a queue.
     */
    public void pushWeights(ArrayList<Float> weights) {
        receivedModels.add(weights);
    }

    /**
     * Simulates sending weights to this node over a network with some latency.
     */
    public double sendTo(int senderID, ArrayList<Float> weights) {
        // Get current latency and sleep for that amount of time.
        double latency = currLatencies[senderID];
        try {
            Thread.sleep((long) (latency * 1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Add weights to queue.
        pushWeights(weights);

        // Update latency by adding a random value between -0.1 and 0.1.
        double delta = 0.2 * CommonState.r.nextDouble() - 0.1;
        currLatencies[senderID] = latency + delta;

        receiveLatency += latency;
        return latency;
    }

    /**
     * Performs a training iteration
     */
    private void train(int id) {
        try {
            InputStream scriptOutput = runScript(trainScriptPath, id, true, Integer.toString(currentIteration),
                    Integer.toString(Constants.ITERATIONS));
            modelWeights = parseWeights(scriptOutput);
        } catch (IOException e) {
            System.err.println("Failed to run " + trainScriptPath);
            e.printStackTrace();
        }
    }

    /**
     * Calculates the test accuracy with the current model weights and stores it
     * in testAccuracy and testLoss.
     */
    private void test(int id) {
        try {
            InputStream scriptOutput = runScript(testScriptPath, id, true);
            Scanner scanner = new Scanner(scriptOutput);
            testAccuracy = scanner.nextDouble();
            testLoss = scanner.nextDouble();
            scanner.close();
        } catch (IOException e) {
            System.err.println("Failed to run " + testScriptPath);
            e.printStackTrace();
        }
    }

    private InputStream runScript(String scriptPath, int id, boolean sendWeights, String... extraArgs)
            throws IOException {
        // Setup arguments.
        String[] constArgs = { "python3", scriptPath, Integer.toString(modelWeights.size()),
                Integer.toString(Constants.NETWORK_SIZE), Integer.toString(id) };
        String[] args = Arrays.copyOf(constArgs, constArgs.length + extraArgs.length);
        for (int i = constArgs.length; i < args.length; i++) {
            args[i] = extraArgs[i - constArgs.length];
        }

        // Initialize process builder redirecting stderr to terminal.
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

        // Start the process.
        Process script = processBuilder.start();
        InputStream input = script.getInputStream();
        OutputStream output = script.getOutputStream();

        // Send weights to stdin if requested.
        if (sendWeights) {
            for (int i = 0; i < modelWeights.size(); i++) {
                byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(modelWeights.get(i))
                        .array();
                output.write(bytes);
            }
            output.flush();
        }

        return input;
    }

    private ArrayList<Float> parseWeights(InputStream input) throws IOException {
        ArrayList<Float> weights = new ArrayList<>();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i += 4) {
                byte[] bytes = { (byte) buffer[i], (byte) buffer[i + 1], (byte) buffer[i + 2], (byte) buffer[i + 3] };
                float weight = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                weights.add(weight);
            }
        }

        return weights;
    }
}
