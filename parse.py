import sys
import matplotlib.pyplot as plt


def avg(lst):
    return sum(lst) / len(lst)


def parse(filename):
    iters = {}  # {iter_num: [[timestamps], [accuracies]]}
    sec = [-1]  # [seconds]

    with open(filename, "r") as f:
        for line in f:
            line = line.strip().split()
            if line[-1] != "seconds":
                timestamp = int(line[0][1:-2])
                iter_num = int(line[line.index("iteration") + 1])
                accuracy = float(line[-1])
                if iter_num in iters:
                    iters[iter_num][0].append(timestamp)
                    iters[iter_num][1].append(accuracy)
                else:
                    iters[iter_num] = [[timestamp], [accuracy]]
            else:
                sec[0] = float(line[-2])

    times, accs = [], []

    for i in range(1, len(iters) + 1):
        timestamps, accuracies = iters[i]
        times.append(avg(timestamps))
        accs.append(avg(accuracies))

    first_time = times[0]
    times = [t - first_time for t in times]
    times = [t / 1000.0 for t in times]

    accs = [a / 100.0 for a in accs]

    return times, accs, sec[0]


def main():
    assert len(sys.argv) >= 2  # Take in list of filenames.
    filenames = sys.argv[1:]

    LABELS = [
        "All-Reduce-1",
        "All-Reduce-2",
        "All-Reduce-4",
        "All-Reduce-8",
        "Gossip-Learning-Random",
        "Gossip-Learning-Latency",
    ]

    count = 0
    for filename in filenames:
        times, accs, sec = parse(filename)
        plt.plot(times, accs, label=LABELS[count])
        print(f"{LABELS[count]}\t{sec}")
        count += 1

    plt.xlabel("Time (sec.)")
    plt.ylabel("Average Accuracy")
    plt.title("Average Accuracy vs. Time")

    plt.legend()

    plt.show()


if __name__ == "__main__":
    main()
