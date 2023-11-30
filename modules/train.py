import sys, torch
from common import batch_size, device, loss_fn, NeuralNetwork, optimizer, training_data
from torch.utils.data import DataLoader, RandomSampler


def main():
    # Parse command-line arguments.
    assert len(sys.argv) == 4
    num_weights, num_nodes, seed = map(int, sys.argv[1:4])

    # Initialize the data loader.
    generator = torch.Generator().manual_seed(seed)
    sampler = RandomSampler(
        training_data,
        num_samples=len(training_data) // num_nodes,
        generator=generator,
    )
    dataloader = DataLoader(
        training_data,
        batch_size=batch_size,
        sampler=sampler,
    )

    # Read the weights from `stdin` as bytes.
    buffer = []
    while len(buffer) < num_weights * 4:
        buffer += sys.stdin.buffer.read()
    input = torch.frombuffer(
        bytearray(buffer), dtype=torch.float32, count=num_weights
    ).tolist()

    # Load the weights into the model.
    i, model = 0, NeuralNetwork().to(device)
    for param in model.parameters():
        weights = input[i : i + param.numel()]
        param.data = torch.tensor(weights).reshape(param.shape).to(device)
        i += param.numel()

    # Train the model.
    model.train()
    for X, y in dataloader:
        X, y = X.to(device), y.to(device)

        # Compute prediction error.
        pred = model(X)
        loss = loss_fn(pred, y)

        # Perform backpropagation.
        loss.backward()
        optimizer(model.parameters(), lr=0.001).step()
        optimizer(model.parameters(), lr=0.001).zero_grad()

    # Flatten the weights.
    weights = []
    for param in model.parameters():
        weights += torch.flatten(param.data).tolist()

    # Dump the weights to `stdout` as bytes.
    sys.stdout.buffer.write(torch.tensor(weights).numpy().tobytes())


if __name__ == "__main__":
    main()
