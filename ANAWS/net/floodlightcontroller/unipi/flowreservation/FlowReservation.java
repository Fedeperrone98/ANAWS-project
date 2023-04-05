package net.floodlightcontroller.unipi.flowreservation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.util.Pair;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.Link;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.Path;
import net.floodlightcontroller.statistics.FlowRuleStats;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.statistics.SwitchPortBandwidth;
import net.floodlightcontroller.topology.ITopologyManagerBackend;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.util.FlowModUtils;

public class FlowReservation implements IFloodlightModule, IOFMessageListener, IFlowReservationREST {

	protected IFloodlightProviderService floodlightProvider; // Reference to the provider
	protected ITopologyManagerBackend topologyManagerBackend; // to retrieve path from src to dest
	protected IDeviceService deviceService; // to retrieve edge switch attached to a host
	protected IOFSwitchService switchService; // to retrieve IOFSwitch given its DatapathID
	protected IStatisticsService statisticsService; // to retrieve statistics
	protected IRestApiService restApiService; // Reference to the Rest API service

	protected static Logger log;
	protected static Logger logREST;

	// seconds after which do periodic check of delivered bytes
	// must be above 11 seconds because the statistics are collected every 10 seconds
	private final static long PERIODIC_CHECK = 15;

	// number of paths to calculate every time an user subscribe a new h2h flow
	private final static int PATHS_NUMBER = 20;

	private final static int BANDWIDTH_HARDCODED = (int) (800 * Math.pow(2, 20)); //800Mbps

	private final static double MIN_THRESHOLD = 1.0025;

	Map<IPv4Address, List<Link>> reservedPaths = new HashMap<>(); // reserved paths of the network
	List<Link> reservedLinks = new ArrayList<>(); // reserved links of the network
	Map<IPv4Address, IPv4Address> requestedH2HFlow = new HashMap<>(); // reserved host to host flow

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

	protected void deallocateH2HFlow(IPv4Address src_ip, List<NodePortTuple> switches, Match match, Match match_ack) {
		
		// update data structure to remove the links of the h2h flow
		removePath(src_ip);
		
		// delete flow rules of all the switches of the path
		removeFlowRules(switches, match, match_ack);
		return;
	}

	// update data structure to remove the links of the reserved path
	protected void removePath(IPv4Address src_ip) {
		
		// remove h2h flow
		if (requestedH2HFlow.containsKey(src_ip)) {
			requestedH2HFlow.remove(src_ip);
		}
		// remove reserved links
		List<Link> links = reservedPaths.get(src_ip);
		if (links != null) {
			for (Link link: links) {
				reservedLinks.remove(link);
			}
		}
		// remove reserved path
		reservedPaths.remove(src_ip);

		log.info("Data structure updated correctly");

		return;
	}
	
	// delete flow rules of all the switches of the path
	protected void removeFlowRules(List<NodePortTuple> switches, Match match, Match match_ack) {
		
		for (int i = 0; i < switches.size(); i++) {

			// retrieve IOFSwitch and OFport of the src switch of each link of the path
			NodePortTuple src = switches.get(i);
			IOFSwitch srcSwitch = switchService.getSwitch(src.getNodeId());
			
			OFFactory factory = srcSwitch.getOFFactory();

			if (i%2 == 0 || i == switches.size() - 1) {	
				OFFlowDelete flowDel = factory.buildFlowDelete()
									.setMatch(match)
									.setIdleTimeout(0)
									.setHardTimeout(0)
									.setPriority(FlowModUtils.PRIORITY_MAX)
									.setBufferId(OFBufferId.NO_BUFFER)
									.build();
							
				srcSwitch.write(flowDel);
			}
			if (i == 0 || i%2 == 1) {
				OFFlowDelete flowDel_ack = factory.buildFlowDelete()
									.setMatch(match_ack)
									.setIdleTimeout(0)
									.setHardTimeout(0)
									.setPriority(FlowModUtils.PRIORITY_MAX)
									.setBufferId(OFBufferId.NO_BUFFER)
									.build();
							
				srcSwitch.write(flowDel_ack);
			}
		}
		log.info("Flow rules removed correctly");
		return;
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		
		return Command.CONTINUE;
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
		l.add(ILinkDiscoveryService.class);
		l.add(IOFSwitchService.class);
		l.add(IStatisticsService.class);
		
		// Add among the dependences the RestApi service
		l.add(IRestApiService.class);

		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// Retrieve pointers to services
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		topologyManagerBackend = (ITopologyManagerBackend) context.getServiceImpl(ITopologyService.class);
		deviceService = context.getServiceImpl(IDeviceService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		statisticsService = context.getServiceImpl(IStatisticsService.class);
		
		// Retrieve a pointer to the rest api service
	    restApiService = context.getServiceImpl(IRestApiService.class);

		log = LoggerFactory.getLogger(FlowReservation.class);
		logREST = LoggerFactory.getLogger(IFlowReservationREST.class);

	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// wait for packet_in
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		
		// Add as REST interface
		restApiService.addRestletRoutable(new FlowReservationWebRoutable());

		// enable the collection of statistics
		statisticsService.collectStatistics(true);

	}

	// get the reserved links of the network
	@Override
	public Map<String, String> getLinksState() {

		logREST.info("Received request for getting the list of reserved links of the network");

		Map<String, String> temp = new HashMap<>();
		for (Link l : reservedLinks) {
			String src = "src_node: " + l.getSrc().toString() + " port: " + l.getSrcPort().toString();
			String dest = "dest_node: " + l.getDst().toString() + " port: " + l.getDstPort().toString();
			temp.put(src, dest);
		}

		return temp;
	}

	// get the reserved paths of the network
	@Override
	public Map<String, String> getPathsState() {

		logREST.info("Received request for getting the list of reserved Paths of the network");

		Map<String, String> temp = new HashMap<>();
		for (Map.Entry<IPv4Address, List<Link>> path: reservedPaths.entrySet()) {
			String src_ip = "src_ip: " + path.getKey().toString();
			String value = "";
			for (Link l : path.getValue()) {
				String src = "src_node: " + l.getSrc().toString() + " port: " + l.getSrcPort().toString();
				String dest = " dest_node: " + l.getDst().toString() + " port: " + l.getDstPort().toString() + "; ";
				value += src + dest;
			}
			temp.put(src_ip, value);
		}

		return temp;
	}

	// get the reserved host-to-host flow
	@Override
	public Map<String, String> getH2HFlow() {

		logREST.info("Received request for getting the list of reserved host-to-host flow");

		Map<String, String> temp = new HashMap<>();
		for (Map.Entry<IPv4Address, IPv4Address> f: requestedH2HFlow.entrySet()) {
			String src = "src: " + f.getKey().toString();
			String dest = "dest: " + f.getValue().toString();
			temp.put(src, dest);
		}
		return temp;
	}

	// install flow rules on the switches belonging to the path
	private void installFlowRules(Path path, IPv4Address src_ip, MacAddress src_mac, MacAddress dest_mac, float dataload, 
									SwitchPort destEdgeSwitchPort, SwitchPort srcEdgeSwitchPort) {
		
		// enable the collection of statistics
		statisticsService.collectStatistics(true);

		log.info("Install flow rules on the switches of the path");
		
		// get the list of switches and ports along the path
		List<NodePortTuple> switches = path.getPath();

		Match match = null;
		Match match_ack = null;
		// iterate over the switches of the path
		for (int i = 0; i < switches.size(); i++) {

			if (i%2 == 0 || i == switches.size() - 1) {	
				// retrieve IOFSwitch and OFport of the src switch of each link of the path
				NodePortTuple src = switches.get(i);
				IOFSwitch srcSwitch = switchService.getSwitch(src.getNodeId());
				OFPort srcPort = src.getPortId();

				// build match: if the srcSwitch receives a packet coming from src_mac and addressed to dest_mac
				OFFactory factory = srcSwitch.getOFFactory();
				match = factory.buildMatch()
							.setExact(MatchField.ETH_SRC, src_mac)
							.setExact(MatchField.ETH_DST, dest_mac)
							.build();

				
				OFActions actions = factory.actions();
				OFActionOutput output = null;
				if (i == switches.size() - 1) {
					// the packet must be sent to destPort (output port of the destSwitch)
					output = actions.buildOutput()
									.setPort(destEdgeSwitchPort.getPortId())
									.build();
				} else {
					// the packet must be sent to srcPort (output port of the srcSwitch)
					output = actions.buildOutput()
									.setPort(srcPort)
									.build();
				}			

				OFFlowAdd flowAdd = factory.buildFlowAdd()
									.setMatch(match)
									.setActions(Collections.singletonList(output))
									.setIdleTimeout(0)
									.setHardTimeout(0)
									.setPriority(FlowModUtils.PRIORITY_MAX)
									.setBufferId(OFBufferId.NO_BUFFER)
									.build();
									
				srcSwitch.write(flowAdd);
			}
						
			// creation of flow entry for the reverse path
			if (i == 0 || i%2 == 1) {
				
				NodePortTuple src_ack = switches.get(i);
				IOFSwitch srcSwitch_ack = switchService.getSwitch(src_ack.getNodeId());
				OFPort srcPort_ack = src_ack.getPortId();
				
				// match for the reverse path
				OFFactory factory = srcSwitch_ack.getOFFactory();
				match_ack = factory.buildMatch()
								.setExact(MatchField.ETH_SRC, dest_mac)
								.setExact(MatchField.ETH_DST, src_mac)
								.build();
				
				OFActions actions_ack = factory.actions();
				OFActionOutput output_ack = null;
				if (i == 0) {
					// the packet must be sent to input port of the first srcSwitch
					output_ack = actions_ack.buildOutput()
									.setPort(srcEdgeSwitchPort.getPortId())
									.build();
				} else {
					
					// the packet must be sent to the source port of the previous switch
					output_ack = actions_ack.buildOutput()
									.setPort(srcPort_ack)
									.build();
				}			

				OFFlowAdd flowAdd_ack = factory.buildFlowAdd()
									.setMatch(match_ack)
									.setActions(Collections.singletonList(output_ack))
									.setIdleTimeout(0)
									.setHardTimeout(0)
									.setPriority(FlowModUtils.PRIORITY_MAX)
									.setBufferId(OFBufferId.NO_BUFFER)
									.build();

				srcSwitch_ack.write(flowAdd_ack);
			}
			
		}

		// start a thread that is responsible to monitor the traffic of this specific path
		log.info("Start a thread responsible to monitor the traffic of this specific path");

		// retrieve link bandwidth to estimate the number of seconds needed to delivery the file to destination
		int sec = 0;
		SwitchPortBandwidth bw = statisticsService.getBandwidthConsumption(destEdgeSwitchPort.getNodeId(), destEdgeSwitchPort.getPortId());
		double dataload_in_bit = dataload * Math.pow(2, 30) * 8;
		if (bw != null) {	
			long speed = bw.getLinkSpeedBitsPerSec().getValue();				
			sec = (int) Math.ceil(dataload_in_bit/speed);					
		} else {
			sec = (int) Math.ceil(dataload_in_bit/BANDWIDTH_HARDCODED);
		}
		Thread t = new Thread(new TrafficMonitor(src_ip, src_mac, dest_mac, dataload, sec, match, match_ack, switches));
		t.start();
	}

	// reserve flow path
	// return error code:
	// -1 --> wrong src ip
	// -2 --> wrong dest ip
	// -3 --> not available path
	// 0 --> available path -> reserved
	@Override
	public int subscribeFlow(IPv4Address src_ip, MacAddress src_mac, IPv4Address dest_ip, MacAddress dest_mac, float dataLoad) {

		// enable the collection of statistics
		statisticsService.collectStatistics(true);
		
		logREST.info("Received request for the subscription of a new h2h flow");
		logREST.info("src_ip: {}", src_ip);
		logREST.info("src_mac: {}", src_mac);
		logREST.info("dest_ip: {}", dest_ip);
		logREST.info("dest_mac: {}", dest_mac);
		logREST.info("dataload: {}GiB", dataLoad);

		// verify that src and dest are hosts belonging to the network
		IDevice src_dev = deviceService.findDevice(
			src_mac, 
			VlanVid.ZERO, 
			src_ip,
			IPv6Address.NONE, 
			DatapathId.NONE,
			OFPort.ZERO);
		if (src_dev == null) {
			logREST.info("Wrong src IP/MAC: there is no host with the given IP/MAC addresses");
			return -1;
		}
		IDevice dest_dev = deviceService.findDevice(
			dest_mac, 
			VlanVid.ZERO, 
			dest_ip,
			IPv6Address.NONE, 
			DatapathId.NONE,
			OFPort.ZERO);
		if (dest_dev == null) {
			logREST.info("Wrong dest IP/MAC: there is no host with the given IP/MAC addresses");
			return -2;
		}

		// retrieve edge switches
		SwitchPort[] srcEdgeSwitches = src_dev.getAttachmentPoints();
		DatapathId srcEdge = null;
		for (SwitchPort sp: srcEdgeSwitches) {
			srcEdge = sp.getNodeId();
		}
		logREST.info("srcEdge: {}", srcEdge.toString());
		SwitchPort[] destEdgeSwitches = dest_dev.getAttachmentPoints();
		DatapathId destEdge = null;
		for (SwitchPort sp: destEdgeSwitches) {
			destEdge = sp.getNodeId();
		}
		if (destEdge != null) {
			logREST.info("destEdge: {}", destEdge.toString());
		} else {
			logREST.info("destEdge is null");
		}

		// check if a path is available
		if (requestedH2HFlow != null && ((requestedH2HFlow.containsKey(src_ip) || requestedH2HFlow.containsKey(dest_ip)) ||
			(requestedH2HFlow.containsValue(src_ip) || requestedH2HFlow.containsValue(dest_ip)))) {
			logREST.info("There is already a path with the given source/destination host");
			return -3;
		}

		if (srcEdge != null && destEdge != null) {
			// compute PATHS_NUMBER possible paths
			List<Path> paths = topologyManagerBackend.getCurrentTopologyInstance().getPathsSlow(srcEdge, destEdge, PATHS_NUMBER);
			boolean available = true;
			for (Path path: paths) {
				available = true;
				List<NodePortTuple> switchPorts = path.getPath(); // List of switch + port belonging to path
				for (NodePortTuple switchPort : switchPorts) { // iterate over the switches of the path
					for (Link link : reservedLinks) {
						NodePortTuple src = new NodePortTuple(link.getSrc(), link.getSrcPort()); // src node of the reserved link
						NodePortTuple dest = new NodePortTuple(link.getDst(), link.getDstPort()); // dest node of the reserved link
						if (switchPort.equals(src) || switchPort.equals(dest)) { // check if the link of the path is not already reserved
							available = false;
							break;
						}
					}
					if (!available) { // the considered path isn't available
						break;
					}
				}
				if (available) { // the path is available
					logREST.info("Found an available path");
					// update data structures to add the links of the path in the list of reserved links
					requestedH2HFlow.put(src_ip, dest_ip);
					List<Link> temp = new ArrayList<>();
					for (int i = 0; i < switchPorts.size(); i++) {
						NodePortTuple src = switchPorts.get(i);
						NodePortTuple dest = switchPorts.get(++i);
						Link link = new Link(src.getNodeId(), src.getPortId(), dest.getNodeId(), dest.getPortId(), null);
						reservedLinks.add(link);
						temp.add(link);
					}
					reservedPaths.put(src_ip, temp);
					
					// install flow rules on the switches along the path
					installFlowRules(path, src_ip, src_mac, dest_mac, dataLoad, destEdgeSwitches[0], srcEdgeSwitches[0]);

					logREST.info("Path reserved correctly");
					return 0; // path reserved correctly
				} else {
					continue; // check if the other path is available
				}
			}
		}
		logREST.info("There isn't an available path");
		return -3;
	}

	protected class TrafficMonitor implements Runnable {

		MacAddress src_mac; // source of the path
		IPv4Address src_ip;
		MacAddress dest_mac; // destination of the path
		float dataload;
		long millisec; // estimated milliseconds needed to delivery the file to destination
		Match match; // match to this specific path
		Match match_ack;
		List<NodePortTuple> switches;

		TrafficMonitor(IPv4Address src_ip, MacAddress src, MacAddress dest, float dataload, int sec, Match match, Match match_ack, List<NodePortTuple> switches) {
			this.src_ip = src_ip;
			this.src_mac = src;
			this.dest_mac = dest;
			this.dataload = dataload;
			this.millisec = sec * 1000;
			this.match = match;
			this.match_ack = match_ack;
			this.switches = switches;

		}

		@Override
		public void run() {
			try {
				log.info("Wait enough time to deliver file to destination");
				Thread.sleep(this.millisec);
			} catch (InterruptedException e) {
				deallocateH2HFlow(src_ip, this.switches, this.match, this.match_ack);
				e.printStackTrace();
			}

			log.info("Retrieve statistics of the last switch of the path with src_ip {}", src_ip);

			int count = 0;
			float prev_byte_delivered = 0;

			while (true) {
				Map<Pair<Match, DatapathId>, FlowRuleStats> all_statistics = statisticsService.getFlowStats();

				// retrieve the last link of the path
				int last_link_index = reservedPaths.get(src_ip).size() - 1;
				Link last_link = reservedPaths.get(src_ip).get(last_link_index);
				// retrieve the DatapathId of the last switch of the path
				DatapathId last_switch_id = last_link.getDst();

				// get the statistics of the last switch for that specific match
				FlowRuleStats stats;
				
				do {
					// enable the collection of statistics
					log.info("Enable statistics: wait collection of statistics");
					statisticsService.collectStatistics(true);
					try {
						// the statistcs are collected every 11 sec
						Thread.sleep(11100);
					} catch (InterruptedException e) {
						deallocateH2HFlow(src_ip, this.switches, this.match, this.match_ack);
						e.printStackTrace();
					}
					
					stats = all_statistics.get(new Pair<Match,DatapathId>(this.match, last_switch_id));
					
				} while (stats == null);

				long bytes_delivered = stats.getByteCount().getValue();
				long packets = stats.getPacketCount().getValue();

				log.info("bytes delivered: {}", bytes_delivered);
				log.info("packets: {}", packets);

				double dataload_in_byte = this.dataload * Math.pow(2, 30);
				if (prev_byte_delivered < bytes_delivered) {
					prev_byte_delivered = bytes_delivered;
				} else if (prev_byte_delivered == bytes_delivered) {
					count++;
				}

				if ((bytes_delivered > dataload_in_byte * MIN_THRESHOLD) || count >= 5) {
						log.info("File delivered to the destination");
						break;
					
				} else {
					log.info("File not delivered to destination yet. Wait until next check");
					try {
						Thread.sleep(PERIODIC_CHECK*1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
						deallocateH2HFlow(src_ip, this.switches, this.match, this.match_ack);
					}
				}

			}
			log.info("Deallocate h2h flow");
			deallocateH2HFlow(src_ip, this.switches, this.match, this.match_ack);
		}
	}
}
