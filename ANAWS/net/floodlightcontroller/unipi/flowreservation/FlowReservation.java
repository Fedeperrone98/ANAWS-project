package net.floodlightcontroller.unipi.flowreservation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;
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
import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.topology.TopologyInstance;
import net.floodlightcontroller.topology.TopologyManager;

public class FlowReservation implements IFloodlightModule, IOFMessageListener, IFlowReservationREST {

	protected IFloodlightProviderService floodlightProvider; // Reference to the provider
	protected static Logger log;
	protected static Logger logREST;
	
	protected IRestApiService restApiService; // Reference to the Rest API service

	protected TopologyManager topologyManager;

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
		// TODO Auto-generated method stub
		return null;
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

		topologyManager = new TopologyManager();

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
	public boolean subscribeFlow(IPv4Address src, IPv4Address dest, float dataLoad) {
		
		logREST.info("Received request for subscribiscription of a new flow");
		logREST.info("src_ip:{}", src);
		logREST.info("dest_ip:{}", dest);
		logREST.info("dataload:{}GB", dataLoad);

		TopologyInstance ti = topologyManager.getCurrentInstance();
        Set<DatapathId> switches = ti.getSwitches();

		Map<DatapathId, Set<Link>> links = topologyManager.getAllLinks();

		// check if path available
		return false;
	}

}
