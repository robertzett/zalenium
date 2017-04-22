package de.zalando.tip.zalenium.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openqa.grid.internal.Registry;
import org.openqa.grid.web.servlet.RegistryBasedServlet;

import com.google.common.io.ByteStreams;

import de.zalando.tip.zalenium.util.Dashboard;

// We use this class name to be able to go to the resource like this: http://localhost:4444/grid/admin/cleanup
public class Cleanup extends RegistryBasedServlet {

    private static final Logger LOGGER = Logger.getLogger(Cleanup.class.getName());

    @SuppressWarnings("unused")
    public Cleanup() {
        this(null);
    }

    public Cleanup(Registry registry) {
        super(registry);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected void process(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String action = "";

        try {
            action = request.getParameter("action");
        } catch (Exception e) {
            LOGGER.log(Level.FINE, e.toString(), e);
        }

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        String resultMsg;
        if (action != null && action.equals("doCleanupAll")) {
            Dashboard.clearRecordedVideos();

            resultMsg = "SUCCESS";
            response.setStatus(200);
        } else {
            resultMsg = "ERROR action not implemented. Given action=" + action;
            response.setStatus(400);
        }

        try (InputStream in = new ByteArrayInputStream(resultMsg.getBytes("UTF-8"))) {
            ByteStreams.copy(in, response.getOutputStream());
        } finally {
            response.getOutputStream().close();
        }
    }
}
