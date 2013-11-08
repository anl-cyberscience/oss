#author - Mark Walters (mwalters@bcmcgroup.com)


from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
import sys
from ssl import wrap_socket, CERT_REQUIRED
import config

if len(sys.argv) != 2:
    print 'Usage: {0} <LISTENING_PORT>'.format(sys.argv[0])
    sys.exit(1)
else:
    try: CALLBACK = int(sys.argv[1])
    except ValueError:
        print 'Callback Port must be an integer'
        sys.exit(1)

# This class will handle any incoming request from the browser 
class myHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        try:
            xml = self.rfile.read(int(self.headers['Content-Length']))
            print xml
            self.send_response(200)
            self.end_headers()
        except Exception as e:
            print "Message receipt failed: {0}".format(e)
            self.send_response(404)
            self.end_headers()

    # to silence the output logs
    def log_message(self, format, *args):
        return

try:
    server = HTTPServer(('', CALLBACK), myHandler)
    server.socket = wrap_socket(server.socket, keyfile=config.KEY, certfile=config.CERT, server_side=True, cert_reqs=CERT_REQUIRED, ca_certs=config.CACERTS)
    print 'Started subscriber on port {0}\n'.format(CALLBACK)
    server.serve_forever()
except KeyboardInterrupt:
    print ' CTRL-C received, shutting down subscriber on port {0}'.format(CALLBACK)
    server.socket.close()
