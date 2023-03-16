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
			
			// Get the field source address
			String src = root.get("src").asText();
			IPv4Address src_ip = IPv4Address.of(src);

			// Get the field destination address
			String dest = root.get("dest").asText();
			IPv4Address dest_ip = IPv4Address.of(dest);

			// Get the field dataload
			float dataLoad = Float.parseFloat(root.get("dataload").asText());
			
			IFlowReservationREST fr = (IFlowReservationREST)getContext().getAttributes()
					.get(IFlowReservationREST.class.getCanonicalName());
			boolean res = fr.subscribeFlow(src_ip, dest_ip, dataLoad);
			if (!res) {
				return new String("Reservation requested denied: No path available");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return new String("OK: path reserved");
	}
}
