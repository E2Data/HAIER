package gr.ntua.ece.cslab.e2datascheduler.ws;

import javax.ws.rs.core.Response;


public class AbstractE2DataService {

    public Response generateResponse(final Response.StatusType httpStatus, final Object obj) {
        final Response ret = Response.status(httpStatus).entity(obj).build();
        return ret;
    }

}
