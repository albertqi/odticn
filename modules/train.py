import sys, torch
from init import train_dataloader, model, device, loss_fn, optimizer

# Read the weights from `stdin`.
input = sys.stdin.readline().strip()
input_weights = [float(w) for w in input.split(" ")]

# Load the weights into the model.
i = 0
for param in model.parameters():
    weights = input_weights[i : i + param.numel()]
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

# Print the weights to `stdout`.
print(" ".join([str(w) for w in weights]))
