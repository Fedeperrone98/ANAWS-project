package net.floodlightcontroller.unipi.flowreservation;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class FlowReservationWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		Router router = new Router(context);

		// add resources to expose
		
		// this resource ask for a flow reservation
		router.attach("/network/reserve/flow/json", SubscribeFlow.class);

		// this resource will show the reserved links of the network
		router.attach("/network/get/reserved/links/json", GetLinksState.class);

		// this resource will show the reserved paths of the network
		router.attach("/network/get/reserved/paths/json", GetPathsState.class);

		// this resource will show the reserved host-to-host flow
		router.attach("/network/get/h2h/flow/json", GetH2HFlow.class);

		return router;
	}

	@Override
	public String basePath() {
		// Root path for the resources
		return "/dc";
	}

}
