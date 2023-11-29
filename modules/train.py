import sys, torch
from init import train_dataloader, model, device, loss_fn, optimizer

# Read the weights from `stdin` as bytes.
input = torch.frombuffer(
    bytearray(sys.stdin.buffer.read()), dtype=torch.float32
).tolist()

# Load the weights into the model.
i = 0
for param in model.parameters():
    weights = input[i : i + param.numel()]
    param.data = torch.tensor(weights).reshape(param.shape).to(device)
    i += param.numel()

# Train the model.
model.train()
for batch, (X, y) in enumerate(train_dataloader):
    X, y = X.to(device), y.to(device)

    # Compute prediction error.
    pred = model(X)
    loss = loss_fn(pred, y)

    # Perform backpropagation.
    loss.backward()
    optimizer.step()
    optimizer.zero_grad()

# Flatten the weights.
weights = []
for param in model.parameters():
    weights += torch.flatten(param.data).tolist()

# Dump the weights to `stdout` as bytes.
sys.stdout.buffer.write(torch.tensor(weights).numpy().tobytes())
