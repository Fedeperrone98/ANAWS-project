package net.floodlightcontroller.unipi.flowreservation;

import org.projectfloodlight.openflow.types.IPv4Address;
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
			
			// Get the field hardtimeout
			//int newValue = Integer.parseInt(root.get("hardtimeout").asText());
			String dest = root.get("dest").asText();
			IPv4Address dest_ip = IPv4Address.of(dest);
			float dataLoad = Float.parseFloat(root.get("dataload").asText());
			String src = getClientInfo().getAddress();
			IPv4Address src_ip = IPv4Address.of(src);

			IFlowReservationREST fr = (IFlowReservationREST)getContext().getAttributes()
					.get(IFlowReservationREST.class.getCanonicalName());
			boolean res = fr.subscribeFlow(dest, dataLoad, src_ip);
			if (!res) {
				return new String("Reservation requested denied: No path available");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return new String("OK: path reserved");
	}
}
