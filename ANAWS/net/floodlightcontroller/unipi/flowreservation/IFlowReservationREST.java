package net.floodlightcontroller.unipi.flowreservation;

import net.floodlightcontroller.core.module.IFloodlightService;

import java.util.Map;

import org.projectfloodlight.openflow.types.IPv4Address;

public interface IFlowReservationREST extends IFloodlightService {
    // metodi da esporre
    // get e post
    // get current network state
    public Map<String, String> getNetworkState();
    // reserve flow path
    public boolean subscribeFlow(IPv4Address dest, float dataLoad, IPv4Address src);
}
