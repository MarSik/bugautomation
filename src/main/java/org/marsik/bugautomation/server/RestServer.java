package org.marsik.bugautomation.server;

import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletException;
import javax.ws.rs.core.Application;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.weld.environment.servlet.Listener;
import org.jboss.weld.environment.servlet.WeldServletLifecycle;
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

    public static RestServer build(int port, BeanManager beanManager) throws ServletException {
        RestServer myServer = new RestServer(port, "0.0.0.0");
        DeploymentInfo di = myServer.deployApplication(null, RestApplication.class)
                .setClassLoader(ClassLoader.getSystemClassLoader())
                .setContextPath("")
                .setDeploymentName("Bug automation")
                .addServletContextAttribute(WeldServletLifecycle.BEAN_MANAGER_ATTRIBUTE_NAME, beanManager)
                .addListeners(Servlets.listener(org.jboss.weld.environment.servlet.Listener.class));
        myServer.deploy(di);
        return myServer;
    }
}
