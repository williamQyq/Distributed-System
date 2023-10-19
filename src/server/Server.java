package server;

import java.io.IOException;

public interface Server {
    int port = 5000;//default port number
    void run() throws IOException;
}
