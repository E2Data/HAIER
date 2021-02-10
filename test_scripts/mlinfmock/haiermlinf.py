#!/usr/bin/env python3
#
# ckatsak, Wed Feb 10 03:21:00 EET 2021

import json
import logging

from flask import Flask, jsonify, request


def ml_inference():
    try:
        req_str = request.get_data()
        logging.debug("Raw incoming request:\t{}".format(req_str))
        mlinfereq = json.loads(req_str.decode("utf-8"))
        logging.debug("Deserialized request:\t{}".format(mlinfereq))
    except Exception as err:
        logging.error(err)
    finally:
        return jsonify(3.14159)


if __name__ == "__main__":
    lf = "%(asctime)s %(name)-8s %(module)s %(levelname)-8s    %(message)s"
    logging.basicConfig(format=lf, level=logging.DEBUG)

    app = Flask(__name__)
    app.add_url_rule(
            "/",
            "ml_inference",
            ml_inference,
            methods=["POST"],
    )
    app.run(
            host="0.0.0.0",
            port=58888,
            threaded=True,
            debug=True,
    )
