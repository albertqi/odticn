import sys, torch
from common import device, loss_fn, NeuralNetwork, test_dataloader


def main():
    # Read the weights from `stdin` as bytes.
    buffer = []
    while len(buffer) < 4:
        buffer += sys.stdin.buffer.read()
    num_weights = int.from_bytes(buffer[:4], byteorder="little")
    while len(buffer) < 4 + (num_weights * 4):
        buffer += sys.stdin.buffer.read()
    input = torch.frombuffer(
        bytearray(buffer[4:]), dtype=torch.float32, count=num_weights
    ).tolist()

    # Load the weights into the model.
    i, model = 0, NeuralNetwork().to(device)
    for param in model.parameters():
        weights = input[i : i + param.numel()]
        param.data = torch.tensor(weights).reshape(param.shape).to(device)
        i += param.numel()

    # Test the model.
    model.eval()
    size = len(test_dataloader.dataset)
    num_batches = len(test_dataloader)
    test_loss = correct = 0
    with torch.no_grad():
        for X, y in test_dataloader:
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
