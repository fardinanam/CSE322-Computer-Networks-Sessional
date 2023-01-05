import matplotlib.pyplot as plt

def plotGraph(xs : list, ys : list, xLabel : str, yLabel : str, title : str, fileName : str):
    fig, ax = plt.subplots()
    # ax.plot(xs, ys, color="blue", linestyle='dashed')
    ax.scatter(xs, ys, color="red")
    ax.grid(True)
    # give plot a title
    ax.set_title(title)
    # make axis labels
    ax.set_xlabel(xLabel)
    ax.set_ylabel(yLabel)
    ax.axhline(y=0, color='k')
    ax.axvline(x=0, color='k')
    # save the plot as a file
    fig.savefig('graphs/'+fileName)
    # close the plot file
    plt.close(fig)

def makeTitleAndCreateGraphs(varyingParam : str, xs : list, throughputs : list, avgDelays : list, deliveryRatios : list, dropRatios):
    xLabel = varyingParam
    
    yLabel = 'Throughput'
    title = varyingParam + ' vs ' + yLabel
    # print(xs, throughputs)
    plotGraph(xs, throughputs, xLabel, yLabel, title, title+'.png')

    yLabel = 'Average Delay'
    title = varyingParam + ' vs ' + yLabel
    plotGraph(xs, avgDelays, xLabel, yLabel, title, title+'.png')

    yLabel = 'Delivery Ratio'
    title = varyingParam + ' vs ' + yLabel
    plotGraph(xs, deliveryRatios, xLabel, yLabel, title, title+'.png')

    yLabel = 'Drop Ratio'
    title = varyingParam + ' vs ' + yLabel
    plotGraph(xs, dropRatios, xLabel, yLabel, title, title+'.png')


if __name__ == "__main__":
    varyingParamIdx = -1 # area = 1, no. of nodes = 2, no. of flows = 3
    areaSizes = []
    nodes = []
    flows = []

    throughput = []
    avgDelay = []
    deliveryRatio = []
    dropRatio = []

    with open('results.txt', 'r') as inputFile:
        for line in inputFile:
            if line.startswith('='):
                varyingParamIdx += 1

                if(varyingParamIdx < 1): continue

                varyingParam = None
                xs = []
                
                if varyingParamIdx == 1:
                    varyingParam = 'Area Size'
                    xs = areaSizes
                elif varyingParamIdx == 2:
                    varyingParam = 'Number of Nodes'
                    xs = nodes
                elif varyingParamIdx == 3:
                    varyingParam = 'Number of Flows'
                    xs = flows
                
                print(f"Varying param: {varyingParam}")
                makeTitleAndCreateGraphs(varyingParam, xs, throughput, avgDelay, deliveryRatio, dropRatio)
                
                throughput = []
                avgDelay = []
                deliveryRatio = []
                dropRatio = []
            elif line.startswith('Area Size'):
                # print(line, line.split(sep=" ")[-1])
                areaSizes.append(int(line.split(sep=" ")[-1]))
            elif line.startswith('Number of Nodes'):
                nodes.append(int(line.split(sep=" ")[-1]))
            elif line.startswith('Number of Flows'):
                flows.append(int(line.split(sep=" ")[-1]))
            elif line.startswith('varying') or line.startswith('-'):
                continue
            else:
                metrices = line.split(sep=" ")

                if len(metrices) < 4:
                    continue

                throughput.append(metrices[0])
                avgDelay.append(metrices[1])
                deliveryRatio.append(metrices[2])
                dropRatio.append(metrices[3])