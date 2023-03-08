package net.floodlightcontroller.unipi.flowreservation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.restserver.IRestApiService;

public class FlowReservation implements IFloodlightModule, IOFMessageListener, IFlowReservationREST {

	protected IFloodlightProviderService floodlightProvider; // Reference to the provider
	protected static Logger log;
	protected static Logger logREST;
	
	protected IRestApiService restApiService; // Reference to the Rest API service

	// IP and MAC address for our logical controller
	private final static IPv4Address CONTROLLER_IP = IPv4Address.of("8.8.8.8");

	@Override
	public String getName() {
		return FlowReservation.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		//get the context
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
                IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			
		IPacket pkt = eth.getPayload();

		// Cast to Packet-In
		OFPacketIn pi = (OFPacketIn) msg;

		// Dissect Packet included in Packet-In
		if (eth.isBroadcast() || eth.isMulticast()) {
			if (pkt instanceof ARP) {
				
				log.info("Processing ARP request");
				
				ARP arpRequest = (ARP) eth.getPayload();
				
				if( arpRequest.getTargetProtocolAddress().compareTo(CONTROLLER_IP) == 0 ){
				
					// Process ARP request
					handleARPRequest(sw, pi, cntx);
					
					// Interrupt the chain
					return Command.STOP;
				}
			}
		} else {
			if (pkt instanceof IPv4) {
				
				log.info("Processing IPv4 packet");
				
				IPv4 ip_pkt = (IPv4) pkt;

				if(ip_pkt.getDestinationAddress().compareTo(CONTROLLER_IP) == 0){
					
					// Process IPv4 packet
					handleIPPacket(sw, pi, cntx);
					
					// Interrupt the chain
					return Command.STOP;
				}
				
			}
		}

		// Interrupt the chain
		return Command.CONTINUE;
	}

	private void handleIPPacket(IOFSwitch sw, OFPacketIn pi,
			FloodlightContext cntx) {
		// TODO	
		// ho ricevuto un packet in perchè lo switch ha ricevuto un pacchetto indirizzato al controller
		// (ip = 8.8.8.8) --> un host prova a fare una post request al controller
		// quindi devo installare su tutti gli switch della rete una rule che indica loro che
		// i pacchetti indirizzati all'ip 8.8.8.8 vanno inoltrati al controller 
	}

	private void handleARPRequest(IOFSwitch sw, OFPacketIn pi,
			FloodlightContext cntx) {
		// TODO
		// ho ricevuto un packet in perchè lo switch ha ricevuto un pacchetto indirizzato al controller
		// (ip = 8.8.8.8) --> un host prova a fare una post request al controller
		// quindi devo installare su tutti gli switch della rete una rule che indica loro che
		// i pacchetti indirizzati all'ip 8.8.8.8 vanno inoltrati al controller 
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFlowReservationREST.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m =
				new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IFlowReservationREST.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		
		// Add among the dependences the RestApi service
		l.add(IRestApiService.class);

		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		//inizializzo le variabili
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		log = LoggerFactory.getLogger(FlowReservation.class);
		logREST = LoggerFactory.getLogger(IFlowReservationREST.class);
		
		// Retrieve a pointer to the rest api service
	    restApiService = context.getServiceImpl(IRestApiService.class);

	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		//mi metto in attesa di packet in
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		
		// Add as REST interface
		restApiService.addRestletRoutable(new FlowReservationWebRoutable());

	}

	// get current network state
	@Override
	public Map<String, String> getNetworkState() {

		logREST.info("Received request for getting the network state");
		
		return null;
	}

	// reserve flow path
	@Override
	public boolean subscribeFlow(IPv4Address dest, float dataLoad, IPv4Address src) {

		logREST.info("Received request for subscribiscription of a new flow:" +
					 " from {}, to {}", src, dest);

		// check if path available
		return false;
	}

}
