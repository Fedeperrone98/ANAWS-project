package net.floodlightcontroller.unipi.flowreservation;

import net.floodlightcontroller.core.module.IFloodlightService;
import java.util.Map;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;

public interface IFlowReservationREST extends IFloodlightService {
    
    // get current the reserved links of the network
    public Map<String, String> getLinksState();
    // get current the reserved paths of the network
    public Map<String, String> getPathsState();
    // get  the reserved host-to-host flow
    public Map<String, String> getH2HFlow();
    // reserve host to host flow path
    public int subscribeFlow(IPv4Address src_ip, MacAddress src_mac, IPv4Address dest_ip, MacAddress dest_mac, float dataLoad);
}
