package gr.ntua.ece.cslab.e2datascheduler.ws;

import javax.ws.rs.core.Response;


public class AbstractE2DataService {

    public Response generateResponse(final Response.StatusType httpStatus, final Object obj) {
        final Response ret = Response
                .status(httpStatus)
                // TODO(ckatsak): Are these headers enough for CORS?
                // FIXME(ckatsak): It would be cleaner if CORS headers were only injected for responses to
                //                 the GUI, and obviously if they were fine-grained rather than wildcards.
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "*")
                .header("Access-Control-Allow-Headers", "*")
                .header("Access-Control-Expose-Headers", "*")
                .entity(obj)
                .build();
        return ret;
    }

}
