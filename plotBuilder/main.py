import sys

import matplotlib.pyplot as plt
import util

if __name__ == '__main__':

    plt.ylabel("average time in milliseconds")

    for filename in sys.argv[1:]:
        info = util.take(filename)
        plt.plot(info.xAxis, info.yAxis, label=info.serverType)

        plt.xlabel(info.testingType)
        title = "number of requests = " + str(info.requestNumber) + '\n'
        if info.testingType != util.TestingType.ARRAY_SIZE:
            title += "size of array = " + str(info.arraySize) + "; "
        if info.testingType != util.TestingType.CLIENTS_NUMBER:
            title += "number of clients = " + str(info.clientsNumber) + "; "
        if info.testingType != util.TestingType.SENDING_DELTA:
            title += "delta of sending = " + str(info.sendingDelta) + "; "
        plt.title(title)

    plt.legend()
    plt.show()
