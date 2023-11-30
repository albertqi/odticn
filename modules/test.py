import sys, torch
from common import batch_size, device, loss_fn, NeuralNetwork, test_data
from torch.utils.data import DataLoader, RandomSampler


def main():
    # Parse command-line arguments.
    assert len(sys.argv) == 4
    num_weights, num_nodes, seed = map(int, sys.argv[1:4])

    # Initialize the data loader.
    generator = torch.Generator().manual_seed(seed)
    sampler = RandomSampler(
        test_data,
        num_samples=len(test_data) // num_nodes,
        generator=generator,
    )
    dataloader = DataLoader(
        test_data,
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

    # Test the model.
    model.eval()
    size, num_batches = len(dataloader.dataset), len(dataloader)
    test_loss = correct = 0
    with torch.no_grad():
        for X, y in dataloader:
            X, y = X.to(device), y.to(device)
            pred = model(X)
            test_loss += loss_fn(pred, y).item()
            correct += (pred.argmax(1) == y).type(torch.float).sum().item()
    test_loss /= num_batches
    correct /= size

    # Print the accuracy and average loss to `stdout`.
    print(f"{(100 * correct):>0.1f} {test_loss:>8f}")


if __name__ == "__main__":
    main()
