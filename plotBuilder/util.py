

class TestingType:
    ARRAY_SIZE = "ARRAY_SIZE"
    CLIENTS_NUMBER = "CLIENTS_NUMBER"
    SENDING_DELTA = "SENDING_DELTA"


class PlotInfo:
    serverType: str
    requestNumber: int
    testingType: str

    lowerBound: int
    upperBound: int
    step: int

    arraySize: int
    clientsNumber: int
    sendingDelta: int

    xAxis: list
    yAxis: list


def take(filename: str):
    plot = PlotInfo()

    with open(filename) as file:
        plot.serverType = file.readline().strip()
        plot.requestNumber = int(file.readline())
        plot.testingType = file.readline().strip()

        plot.lowerBound = int(file.readline())
        plot.upperBound = int(file.readline())
        plot.step = int(file.readline())

        plot.arraySize = int(file.readline())
        plot.clientsNumber = int(file.readline())
        plot.sendingDelta = int(file.readline())

        plot.xAxis = []
        plot.yAxis = []

        for_x = True
        for line in file:
            data = int(line)
            if for_x:
                plot.xAxis.append(data)
            else:
                plot.yAxis.append(data)
            for_x = for_x ^ True

    return plot
