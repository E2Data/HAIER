package gr.ntua.ece.cslab.e2datascheduler.ws;

import javax.ws.rs.core.Response;

public class AbstractE2DataService {

    public Response generateResponse(Response.StatusType httpStatus, Object obj) {
        Response response = Response.status(httpStatus).entity(obj).build();
        return response;
    }

}
