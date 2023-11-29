import sys, torch
from common import device, loss_fn, NeuralNetwork, optimizer, train_dataloader


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

    # Train the model.
    model.train()
    for X, y in train_dataloader:
        X, y = X.to(device), y.to(device)

        # Compute prediction error.
        pred = model(X)
        loss = loss_fn(pred, y)

        # Perform backpropagation.
        loss.backward()
        optimizer(model.parameters()).step()
        optimizer(model.parameters()).zero_grad()

    # Flatten the weights.
    weights = []
    for param in model.parameters():
        weights += torch.flatten(param.data).tolist()

    # Dump the weights to `stdout` as bytes.
    sys.stdout.buffer.write(len(weights).to_bytes(4, byteorder="little"))
    sys.stdout.buffer.write(torch.tensor(weights).numpy().tobytes())
    sys.stdout.flush()


if __name__ == "__main__":
    main()
