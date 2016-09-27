package org.marsik.bugautomation.bugzilla;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BugzillaClient {
    private static final Logger logger = LoggerFactory.getLogger(BugzillaClient.class);

    private final URL xmlRpcUrl;
    private XmlRpcClient client;
    private AuthorizationCallback authorizationCallback;
    private String token;

    public BugzillaClient(String baseUrl) throws MalformedURLException {
        xmlRpcUrl = new URL(baseUrl + "/xmlrpc.cgi");
    }

    private XmlRpcClient getClient() {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(xmlRpcUrl);
        config.setContentLengthOptional(true);
        config.setEnabledForExtensions(true);
        config.setReplyTimeout(30000);

        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        //client.setTransportFactory(new XmlRpcCommonsTransportFactory(client));
        return client;
    }

    public void setAuthorizationCallback(AuthorizationCallback authorizationCallback) {
        this.authorizationCallback = authorizationCallback;
    }

    private class Call {
        private final String method;
        private final Multimap<String,Object> arguments = ArrayListMultimap.create();

        public Call(String method) {
            this.method = method;
        }

        public Call argument(String key, Object value) {
            arguments.put(key, value);
            return this;
        }

        public Call arguments(Multimap<String, Object> values) {
            arguments.putAll(values);
            return this;
        }

        public Map<String, Object> call() {
            Map<String, Object> flatArgs = new HashMap<>();
            for (String key: arguments.keySet()) {
                Collection<Object> values = arguments.get(key);
                if (values.size() == 1) {
                    flatArgs.put(key, values.iterator().next());
                } else {
                    flatArgs.put(key, values);
                }
            }

            if (token != null) {
                flatArgs.put("Bugzilla_token", token);
            }

            Object[] callArgs = new Object[] {flatArgs};
            logger.info("Calling bugzilla method {} with args {}", method, callArgs);
            try {
                return (Map<String,Object>)client.execute(method, callArgs);
            } catch (XmlRpcException e) {
                logger.error("Bugzilla RPC call failed: {}", e);
                return Collections.emptyMap();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public boolean open() {
        client = getClient();

        if (authorizationCallback != null) {
            Map<String, Object> ret = null;
            ret = new Call("User.login")
                    .argument("login", authorizationCallback.getName())
                    .argument("password", authorizationCallback.getPassword())
                    .call();
            token = (String)ret.get("token");
        }

        return true;
    }

    public void close() {
        token = null;
        client = null;
    }

    public boolean isLoggedIn() {
        return client != null
                && token != null;
    }

    public String getBugzillaVersion() {
        checkLoggedIn();
        return (String)(new Call("Bugzilla.version").call().get("version"));
    }

    private void checkLoggedIn() {
        if (!isLoggedIn()) {
            logger.error("Not logged into {}", xmlRpcUrl);
            throw new IllegalArgumentException("Not logged in.");
        }
    }

    @SuppressWarnings("unchecked")
    public Iterable<BugProxy> searchBugs(Multimap<String, Object> params) {
        checkLoggedIn();
        Map<String, Object> ret = new Call("Bug.search")
                .arguments(params)
                .call();

        Collection<Map<String,Object>> bugs = Arrays.asList((Object[]) ret.get("bugs"))
                .stream().map(o -> (Map<String, Object>)o).collect(Collectors.toList());

        return bugs.stream().map(BugProxy::new).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public Iterable<BugProxy> getBugs(Collection<String> ids) {
        checkLoggedIn();
        Map<String, Object> ret = new Call("Bug.get")
                .argument("ids", new ArrayList<>(ids))
                .argument("permissive", true)
                .call();

        Collection<Map<String,Object>> bugs = Arrays.asList((Object[]) ret.get("bugs"))
                .stream().map(o -> (Map<String, Object>)o).collect(Collectors.toList());

        return bugs.stream().map(BugProxy::new).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public Iterable<BugProxy> getExtra(Collection<String> ids) {
        checkLoggedIn();
        Map<String, Object> ret = new Call("Bug.get")
                .argument("ids", new ArrayList<>(ids))
                .argument("permissive", true)

                .argument("include_fields", "id")
                .argument("include_fields", "flags")
                .call();

        Collection<Map<String,Object>> bugs = Arrays.asList((Object[]) ret.get("bugs"))
                .stream().map(o -> (Map<String, Object>)o).collect(Collectors.toList());

        return bugs.stream().map(BugProxy::new).collect(Collectors.toList());
    }
}
