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

package org.kantega.reststop.security;

import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.kantega.reststop.api.FilterPhase;
import org.kantega.reststop.api.Reststop;
import org.kantega.reststop.jaxrsapi.DefaultJaxRsPlugin;

/**
 *
 */
public class SecurityPlugin extends DefaultJaxRsPlugin {
    public SecurityPlugin(Reststop reststop) {
        addServletFilter(reststop.createFilter(new BasicAuthFilter(), "/*", FilterPhase.AUTHENTICATION));
        addJaxRsContainerClass(RolesAllowedDynamicFeature.class);
    }
}
