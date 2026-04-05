import sys, SocketServer, select, struct
import datetime

class AMCBoard:
    DPRAM_SIZE = 1024
    dpram = bytearray(DPRAM_SIZE)
    @classmethod
    def write(cls, msg):
        if len(msg) == 12 : # AMC_REGSRW_IRP
            offset, data, success =  struct.unpack('LLL', msg)
            print 'write [%d]=%d' % (offset, data)
            cls.dpram[offset] = struct.pack('B', data)
        else : # AMC_REGSRWGRP_IRP
            offset, count, success = struct.unpack('LLL', msg[:12])
            data = msg[12:]
            print 'write [%d:%d] =' %( offset, offset+count), repr(data)
            cls.dpram[offset:offset+count] = data

    @classmethod
    def read(cls, msg):
        offset, count = struct.unpack('LL', msg)
        if count == 0 : count = 1
        if count == 1:
            print 'read [%d]' % offset
        else:
            print 'read [%d:%d]' % (offset, offset+count)        
        return cls.dpram[offset:offset+count]

class AMCHandler(SocketServer.BaseRequestHandler):
    """
    The request handler class for our server.

    It is instantiated once per connection to the server, and must
    override the handle() method to implement communication to the
    client.
    """

    def handle(self):
        # self.request is the TCP socket connected to the client
        print ">>> Connection from :", self.client_address[0], "at port", self.client_address[1]

        while True:
            i, o, e = select.select([self.request], [], [])
            try: #try
                header = self.request.recv(4)
                magic, length = struct.unpack('HH', header)
                #print 'magic = %02x, len = %d' % (magic, length)
                if magic != 0x7F7F :
                    print 'Wronge magic!', 'magic = %02x, len = %d' % (magic, length)
                    continue
                msg = ''
                while length > 0 :
                    segment = self.request.recv(length)
                    msg += segment
                    length -= len(segment)
                print datetime.datetime.now(), 'received:', len(msg)
                if len(msg) == 8 : # read
                    data = AMCBoard.read(msg)
                    self.request.send(data)
                else:
                    AMCBoard.write(msg)
                    pass
            except :
                print "Unexpected error:", sys.exc_info()[0]
                break

        self.request.close()
        print "<<< Close connection from :", self.client_address[0], "at port", self.client_address[1]


if __name__ == "__main__":
    HOST, PORT = "localhost", 9000

    # Create the server, binding to localhost on port 9999
    server = SocketServer.TCPServer((HOST, PORT), AMCHandler)

    # Activate the server; this will keep running until you
    # interrupt the program with Ctrl-C
    server.serve_forever()