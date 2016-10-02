package org.marsik.bugautomation.server;

import javax.servlet.ServletException;
import javax.ws.rs.core.Application;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.marsik.bugautomation.rest.RestApplication;

public class RestServer {
    private final UndertowJaxrsServer server = new UndertowJaxrsServer();

    public RestServer(Integer port, String host) {
        Undertow.Builder serverBuilder = Undertow.builder().addHttpListener(port, host);
        server.start(serverBuilder);
    }

    public DeploymentInfo deployApplication(String appPath, Class<? extends Application> applicationClass) {
        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setInjectorFactoryClass("org.jboss.resteasy.cdi.CdiInjectorFactory");
        deployment.setApplicationClass(applicationClass.getName());
        return server.undertowDeployment(deployment, appPath);
    }

    public void deploy(DeploymentInfo deploymentInfo) throws ServletException {
        server.deploy(deploymentInfo);
    }

    public static RestServer build(int port) throws ServletException {
        RestServer myServer = new RestServer(port, "0.0.0.0");
        DeploymentInfo di = myServer.deployApplication("/", RestApplication.class)
                .setClassLoader(RestServer.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("Bug automation")
                //.addServlets(Servlets.servlet("helloServlet", org.viddu.poc.HelloServlet.class).addMapping("/hello"))
                .addListeners(Servlets.listener(org.jboss.weld.environment.servlet.Listener.class));
        myServer.deploy(di);
        return myServer;
    }
}
