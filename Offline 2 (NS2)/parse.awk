BEGIN {
    receivedPackets = 0;
    sentPackets = 0;
    droppedPackets = 0;
    totalReceivedBytes = 0;

    totalDelay = 0;
    simStartTime = 1000000;
    simEndTime = 0;

    headerBytes = 20;
}

{
    eventType = $1;
    eventStartTime = $2;
    nodeID = $3;
    layerType = $4;
    packetId = $6;
    packetType = $7;
    packetBytes = $8;

    # Eliminate the underscores from the node ID
    sub(/^_*/, "", node)
    sub(/_*$/, "", node)

    # Set start time for the simulation
    if (eventStartTime < simStartTime) {
        simStartTime = eventStartTime;
    }

    if (layerType == "AGT" && packetType == "exp") {
        if (eventType == "s") {
            sentPackets++;
            packetSentTime[packetId] = eventStartTime;
        } else if (eventType == "r") {
            packetTransmitTime = eventStartTime - packetSentTime[packetId];

            if (packetTrasnmitTime < 0) {
                print "ERROR";
            } else {
                totalDelay += packetTransmitTime;
                totalReceivedBytes += packetBytes - headerBytes;
                receivedPackets++;
            }
        }
    }

    if (eventType == "D") {
        droppedPackets++;
    }
}

END {
    simEndTime = eventStartTime;
    simTime = simEndTime - simStartTime;

    # print "Simulation Time: ", simTime;
    # print "Total Packets Sent: ", sentPackets;
    # print "Total Packets Received: ", receivedPackets;
    # print "Total Packets Dropped: ", droppedPackets;

    # print "------------------------------------------------------";
    # print "Throughput:", (totalReceivedBytes * 8) / simTime, "bits/sec";
    # print "Average Delay:", (totalDelay / receivedPackets), "sec";
    # print "Delivery Ratio:", (receivedPackets / sentPackets);
    # print "Drop Ratio:", (droppedPackets / sentPackets);
    throughput = (totalReceivedBytes * 8) / simTime;
    avgDelay = (totalDelay / receivedPackets);
    deliveryRatio = (receivedPackets / sentPackets);
    dropRatio = (droppedPackets / sentPackets);

    print throughput, avgDelay, deliveryRatio, dropRatio;
}