#Create a simulator object
set ns [new Simulator]

$ns color 1 Blue
$ns color 2 Red

#Open the nam trace file
set namFile [open animation.nam w]
$ns namtrace-all $namFile

#Define a 'finish' procedure
proc finish {} {
        global ns namFile
        $ns flush-trace
	#Close the trace file
        close $namFile
	#Execute nam on the trace file
        exec nam animation.nam &
        exit 0
}

set n0 [$ns node]
set n1 [$ns node]
set n2 [$ns node]
set n3 [$ns node]

$ns duplex-link $n0 $n2 1Mb 10ms DropTail
$ns duplex-link $n1 $n2 1Mb 10ms DropTail
$ns duplex-link $n3 $n2 1Mb 10ms DropTail

# orient the nodes in nam 
$ns duplex-link-op $n0 $n2 orient right-down
$ns duplex-link-op $n1 $n2 orient right-up 
$ns duplex-link-op $n3 $n2 orient left

# Monitor the queue in nam
$ns duplex-link-op $n2 $n3 queuePos 0.5

# Attach an UDP agent with n0
set udp0 [new Agent/UDP]
$ns attach-agent $n0 $udp0
$udp0 set class_ 1      ;#color 1 is blue

set cbr0 [new Application/Traffic/CBR]
$cbr0 set packetSize_ 500
$cbr0 set interval_ 0.005
$cbr0 attach-agent $udp0

# Attach an UDP agent with n1
set udp1 [new Agent/UDP]
$ns attach-agent $n1 $udp1
$udp1 set class_ 2      ;#color 2 is red

set cbr1 [new Application/Traffic/CBR]
$cbr1 set packetSize_ 500
$cbr1 set interval_ 0.005
$cbr1 attach-agent $udp1

# Attach a null agent with n3
set null0 [new Agent/Null]
$ns attach-agent $n3 $null0

# Connect the UDP agents with null agent
$ns connect $udp0 $null0
$ns connect $udp1 $null0

# Schedule events
$ns at 0.5 "$cbr0 start" 
$ns at 1.0 "$cbr1 start"
$ns at 4.0 "$cbr1 stop"
$ns at 4.5 "$cbr0 stop"

#Call the finish procedure after 5 seconds simulation time
$ns at 5.0 "finish"

#Run the simulation
$ns run


