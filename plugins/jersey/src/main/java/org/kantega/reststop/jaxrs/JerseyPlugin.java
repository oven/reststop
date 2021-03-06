/*
 * Copyright 2015 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kantega.reststop.jaxrs;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;
import org.kantega.reststop.api.*;
import org.kantega.reststop.jaxrsapi.JaxRsPlugin;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;

/**
 *
 */
public class JerseyPlugin extends DefaultReststopPlugin {


    private ServletContainer filter;
    private Set<Integer> currentPlugins = new HashSet<>();

    public JerseyPlugin(final Reststop reststop, final ReststopPluginManager pluginManager) throws ServletException {


        addPluginListener(new PluginListener() {
            @Override
            public void pluginsUpdated(Collection<ReststopPlugin> plugins) {
                reloadFromPlugins(plugins);
            }

            @Override
            public void pluginManagerStarted() {
                reloadFromPlugins(pluginManager.getPlugins());
            }

            private void reloadFromPlugins(Collection<ReststopPlugin> plugins) {
                synchronized (JerseyPlugin.this) {
                    Set<Integer> newJaxRsPlugins = new HashSet<>();
                    for (ReststopPlugin plugin : plugins) {
                        if(plugin instanceof JaxRsPlugin) {
                            newJaxRsPlugins.add(System.identityHashCode(plugin));
                        }
                    }

                    if(!newJaxRsPlugins.equals(currentPlugins)) {
                        currentPlugins = newJaxRsPlugins;
                        try {
                            if (filter == null) {
                                filter = addJerseyFilter(new ReststopApplication(plugins));
                                filter.init(reststop.createFilterConfig("jersey", new Properties()));

                                addServletFilter(reststop.createFilter(filter, "/*", FilterPhase.USER));
                            } else {
                                filter.reload(getResourceConfig(new ReststopApplication(plugins)));
                            }
                        } catch (ServletException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }

            }
        });
    }

    private ServletContainer addJerseyFilter(Application application) {
        ResourceConfig resourceConfig = getResourceConfig(application);

        return new ServletContainer(resourceConfig) {
            @Override
            public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
                // Force read of request parameters if specified, otherwise Jersey will eat them
                if(request.getMethod().equals("POST") && MediaType.APPLICATION_FORM_URLENCODED.equals(request.getContentType())) {
                    request.getParameterMap();
                }
                super.doFilter(request, response, chain);
            }
        };
    }


    private ResourceConfig getResourceConfig(Application application) {
        ResourceConfig resourceConfig = ResourceConfig.forApplication(application);
        resourceConfig.register(JacksonFeature.class);
        Map<String, Object> props = new HashMap<>(resourceConfig.getProperties());
        props.put(ServletProperties.FILTER_FORWARD_ON_404, "true");
        resourceConfig.setProperties(props);

        return resourceConfig;
    }
}
