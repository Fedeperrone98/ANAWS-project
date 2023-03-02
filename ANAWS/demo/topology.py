"""
Host: H_x_x_x
Edge switch: es_x_x
Aggregate switch: as_x_x
Core switch: cs_x
"""

from mininet.topo import Topo

class FatTree(Topo):

    def __init__(self):

        # Topology settings
        K = 4                           # K-ary FatTree
        podNum = K                      # Pod number in FatTree
        coreSwitchNum = pow((K/2),2)    # Core switches
        aggrSwitchNum = ((K/2)*K)       # Aggregation switches
        edgeSwitchNum = ((K/2)*K)       # Edge switches
        hostNum = (K*pow((K/2),2))      # Hosts in K-ary FatTree

        # Initialize topology
        Topo.__init__(self)

        coreSwitches = []
        aggrSwitches = []
        edgeSwitches = []

        # Core
        for core in range(0, coreSwitchNum):
            coreThis = self.addSwitch('cs_{}'.format(core), dpid='{:010X}'.format(core+1))
            coreSwitches.append(coreThis)

        # Pod
        for pod in range(0, podNum):
        # Aggregate
            for aggr in range(0, aggrSwitchNum/podNum):
                aggrThis = self.addSwitch('as_{}_{}'.format(pod, aggr), dpid='{:06X}{:02X}{:02X}'.format(1, pod, aggr))
                aggrSwitches.append(aggrThis)
                for x in range((K/2)*aggr, (K/2)*(aggr+1)):
                    self.addLink(aggrThis, coreSwitches[x])

        # Edge
            for edge in range(0, edgeSwitchNum/podNum):
                edgeThis = self.addSwitch('es_{}_{}'.format(pod, edge), dpid='{:06X}{:02X}{:02X}'.format(2, pod, edge))
                edgeSwitches.append(edgeThis)
                for x in range((edgeSwitchNum/podNum)*pod, ((edgeSwitchNum/podNum)*(pod+1))):
                    self.addLink(edgeThis, aggrSwitches[x])

        # Host
                for x in range(0, (hostNum/podNum/(edgeSwitchNum/podNum))):
                    mac = '02:00:00:{:02}:{:02}:{:02}'.format(pod, edge, x)
                    host = self.addHost('h_{}_{}_{}'.format(pod, edge, x), mac=mac, ip='10.{}.{}.{}'.format(pod, edge, x+1))
                    self.addLink(edgeThis, host)


topos = { 'fattree': ( lambda: FatTree() ) }