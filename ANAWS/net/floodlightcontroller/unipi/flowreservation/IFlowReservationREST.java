package net.floodlightcontroller.unipi.flowreservation;

import net.floodlightcontroller.core.module.IFloodlightService;

import java.util.Map;

public interface IFlowReservationREST extends IFloodlightService {
    // metodi da esporre
    // get e post
    // get current network state
    public Map<String, String> getNetworkState();
    // reserve flow path
    public boolean subscribeFlow(String dest, float dataLoad);
}
