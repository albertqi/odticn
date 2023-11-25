from init import train_dataloader, model, device, loss_fn, optimizer


size = len(train_dataloader.dataset)
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
