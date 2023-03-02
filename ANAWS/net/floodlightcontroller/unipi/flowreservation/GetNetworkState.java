package net.floodlightcontroller.unipi.flowreservation;

import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class GetNetworkState extends ServerResource {
	@Get("json")
	public Map<String, String> get() {
		IFlowReservationREST fr =
					(IFlowReservationREST)getContext().getAttributes()
					.get(IFlowReservationREST.class.getCanonicalName());
		return fr.getNetworkState();
	}
}