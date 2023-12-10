import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;

import peersim.cdsim.CDProtocol;
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
     * Tracks when to end the simulation.
     */
    public static final CountDownLatch simulationComplete = new CountDownLatch(1);

    /**
     * The weights for the current model iteration.
     */
    protected ArrayList<Float> modelWeights;

    /**
     * The models that this node has reveiced from peers since the last cycle.
     */
    protected ConcurrentLinkedDeque<ArrayList<Float>> receivedModels;

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
     * Worker thread that performs the training/weight sharing loops.
     */
    private Thread operationsThread;

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
        receivedModels = new ConcurrentLinkedDeque<>();

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
        if (operationsThread == null) {
            operationsThread = new Thread(() -> {
                while (true) {
                    modelWeights = train(node.getIndex());
                    testAccuracy = test(node.getIndex());
                    currentIteration++;

                    shareWeights(node, protocolID);
                    System.out.println("Node " + node.getID() + ": iteration " + currentIteration + " complete. Accuracy = " + testAccuracy);

                    if (testAccuracy > 90) {
                        System.exit(0);
                    }
                }
            });
            System.out.println("Starting worker thread for node " + node.getID());
            operationsThread.start();
            return;
        }

        // Put the simulator to sleep.
        try {
            simulationComplete.await();
        } catch (InterruptedException e) {
            // Do nothing...
        }
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
     * Simulates sending weights to this node over a network with some latency.
     */
    public double sendTo(ArrayList<Float> weights) {
        receivedModels.add(weights);

        // TODO: Generate random latency.
        double latency = 0.0;

        receiveLatency += latency;

        return latency;
    }

    /**
     * Performs a training iteration
     */
    private ArrayList<Float> train(int id) {
        try {
            InputStream scriptOutput = runScript(trainScriptPath, id, true, Integer.toString(currentIteration),
                    Integer.toString(Constants.ITERATIONS));
            return parseWeights(scriptOutput);
        } catch (IOException e) {
            System.err.println("Failed to run " + trainScriptPath);
            e.printStackTrace();
            return modelWeights;
        }
    }

    /**
     * Calculates the test accuracy with the current model weights and stores it
     * in testAccuracy and testLoss.
     */
    private double test(int id) {
        try {
            InputStream scriptOutput = runScript(testScriptPath, id, true);
            Scanner scanner = new Scanner(scriptOutput);
            testAccuracy = scanner.nextDouble();
            testLoss = scanner.nextDouble();
            scanner.close();
            return testAccuracy;
        } catch (IOException e) {
            System.err.println("Failed to run " + testScriptPath);
            e.printStackTrace();
            return this.testAccuracy;
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
