package net.floodlightcontroller.unipi.flowreservation;

import org.restlet.resource.ServerResource;

import java.io.IOException;

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
			float dataLoad = Float.parseFloat(root.get("dataload").asText());
			
			IFlowReservationREST fr = (IFlowReservationREST)getContext().getAttributes()
					.get(IFlowReservationREST.class.getCanonicalName());
			boolean res = fr.subscribeFlow(dest, dataLoad);
			if (!res) {
				return new String("Reservation requested denied: No path available");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return new String("OK: path reserved");
	}
}
