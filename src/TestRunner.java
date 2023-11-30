import java.io.IOException;

public class TestRunner {
    public static void main(String... args) throws IOException {
        TestNode node = new TestNode("name");
        System.err.println("One training iter...");
        node.train(1);
        System.err.println("Testing...");
        node.test(1);

        System.err.println("Accuracy: " + node.test_accuracy + ", Loss: " +
        node.test_loss);
    }
}
