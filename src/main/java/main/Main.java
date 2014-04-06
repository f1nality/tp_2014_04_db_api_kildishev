package main;

import frontend.Frontend;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.Map;

/**
 * @author d.kildishev
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Server server = runWebServer();

        server.join();
    }

    public static Server runWebServer() throws Exception {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

        Frontend frontend = new Frontend();
        context.addServlet(new ServletHolder(frontend), "/*");

        Server server = new Server(80);
        server.setHandler(context);

        QueuedThreadPool threadPool = (QueuedThreadPool)server.getThreadPool();
        threadPool.setMinThreads(10);
        threadPool.setMaxThreads(200);

        server.start();

        return server;
    }
}
