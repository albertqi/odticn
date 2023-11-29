import sys, torch
from init import test_dataloader, model, device, loss_fn

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
