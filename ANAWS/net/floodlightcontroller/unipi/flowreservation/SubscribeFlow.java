package net.floodlightcontroller.unipi.flowreservation;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SubscribeFlow extends ServerResource {
    
    @Post
	public String store(String fmJson) {
		
		// Check if the payload is provided
		if(fmJson == null){
			return new String("No attributes");
		}
		
		// Parse the JSON input
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode root = mapper.readTree(fmJson);
			
			// Get the field source ip address
			String srcIpStr = root.get("srcIp").asText();
			IPv4Address src_ip = IPv4Address.of(srcIpStr);
			// Get the field source mac address
			String srcMacStr = root.get("srcMac").asText();
			MacAddress src_mac = MacAddress.of(srcMacStr);

			// Get the field destination ip address
			String destIpStr = root.get("destIp").asText();
			IPv4Address dest_ip = IPv4Address.of(destIpStr);
			// Get the field destination mac address
			String destMacStr = root.get("destMac").asText();
			MacAddress dest_mac = MacAddress.of(destMacStr);

			// Get the field dataload
			float dataLoad = Float.parseFloat(root.get("dataload").asText());
			
			IFlowReservationREST fr = (IFlowReservationREST)getContext().getAttributes()
					.get(IFlowReservationREST.class.getCanonicalName());
			int res = fr.subscribeFlow(src_ip, src_mac, dest_ip, dest_mac, dataLoad);
			if(res == -1){
				return new String("Reservation requested denied: Wrong src IP/MAC, there isn't any host with this IP/MAC address");
			}else if (res == -2){
				return new String("Reservation requested denied: Wrong dest IP/MAC, there isn't any host with this IP/MAC address");
			}else if (res == -3) {
				return new String("Reservation requested denied: No path available");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			return new String("An exception occurred while parsing the parameters");
		}
		return new String("OK: path reserved");
	}
}