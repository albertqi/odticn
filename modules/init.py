import sys, torch
from common import device, NeuralNetwork


def main():
    # Initialize the model.
    model = NeuralNetwork().to(device)

    # Flatten the weights.
    weights = []
    for param in model.parameters():
        weights += torch.flatten(param.data).tolist()

    # Dump the weights to `stdout` as bytes.
    sys.stdout.buffer.write(torch.tensor(weights).numpy().tobytes())


if __name__ == "__main__":
    main()
