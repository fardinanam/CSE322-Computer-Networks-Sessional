set ns [new Simulator]

set namFile [open animation.nam w]
$ns namtrace-all $namFile

# A proc to finish the trace file and start nam
proc finish {} {
    global ns namFile
    $ns flush-trace
    close $namFile
    exec nam animation.nam &
    exit 0
}

# Create two nodes
set n0 [$ns node]
set n1 [$ns node]

#Connect the nodes with a link
$ns duplex-link $n0 $n1 1Mb 10ms DropTail ;#duplex-link with bw 1Mb, delay 10ms and queue type DropTail

# Create a UDP agent and attach it to node n0
set udp0 [new Agent/UDP]
$ns attach-agent $n0 $udp0

# Create a CBR traffic source and attach it to the UDP agent
set cbr0 [new Application/Traffic/CBR]
$cbr0 set packetSize_ 500
$cbr0 set interval_ 0.005
$cbr0 attach-agent $udp0

# Create a null agent which acts as a traffic sink and attach it to node n1
set null0 [new Agent/Null]
$ns attach-agent $n1 $null0

# Connect the two agents
$ns connect $udp0 $null0

# tell the cbr agent when to start and stop
$ns at 0.5 "$cbr0 start"    ;#this is the syntax for scheduling an event
$ns at 4.5 "$cbr0 stop"

# Execute the finish proc after 5 sec of simulation time
$ns at 5.0 "finish" 

#start the simulation
$ns run

