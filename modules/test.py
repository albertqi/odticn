import torch
from init import test_dataloader, model, device, loss_fn


size = len(test_dataloader.dataset)
num_batches = len(test_dataloader)
model.eval()
test_loss, correct = 0, 0
with torch.no_grad():
    for X, y in test_dataloader:
        X, y = X.to(device), y.to(device)
        pred = model(X)
        test_loss += loss_fn(pred, y).item()
        correct += (pred.argmax(1) == y).type(torch.float).sum().item()
test_loss /= num_batches
correct /= size

print(f"Test Error: \n Accuracy: {(100*correct):>0.1f}%, Avg. Loss: {test_loss:>8f} \n")
