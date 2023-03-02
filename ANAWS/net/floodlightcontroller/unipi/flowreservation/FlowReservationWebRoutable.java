package net.floodlightcontroller.unipi.flowreservation;

import org.restlet.Context;
import org.restlet.Restlet;

import net.floodlightcontroller.restserver.RestletRoutable;

public class FlowReservationWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		Router router = new Router(context);

		// add resources to expose
		
		// this resource ask for a flow reservation
		router.attach("/network/flow/json", SubscribeFlow.class);

		// this resource will show the current state of the network (with reserved paths)
		router.attach("/network/state/json", GetNetworkState.class);

		return router;
	}

	@Override
	public String basePath() {
		// Root path for the resources
		return "/dc";
	}

}
