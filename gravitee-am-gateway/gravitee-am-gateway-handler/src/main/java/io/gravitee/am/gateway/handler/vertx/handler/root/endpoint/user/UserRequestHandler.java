/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.am.gateway.handler.vertx.handler.root.endpoint.user;

import com.google.common.net.HttpHeaders;
import io.gravitee.am.gateway.handler.vertx.utils.UriBuilderRequest;
import io.gravitee.am.identityprovider.api.DefaultUser;
import io.gravitee.am.identityprovider.api.User;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class UserRequestHandler implements Handler<RoutingContext> {

    private static final Logger logger = LoggerFactory.getLogger(UserRequestHandler.class);

    @Override
    public abstract void handle(RoutingContext event);

    protected void redirectToPage(RoutingContext context, Map<String, String> params, Throwable... exceptions) {
        try {
            if (exceptions != null && exceptions.length > 0) {
                logger.debug("Error user actions : " + params.get("error"), exceptions[0]);
            }
            String uri = UriBuilderRequest.resolveProxyRequest(context.request(), context.request().path(), params);
            doRedirect(context.response(), uri);
        } catch (Exception ex) {
            logger.error("An error occurs while redirecting to {}", context.request().absoluteURI(), ex);
            context.fail(503);
        }
    }

    protected User getAuthenticatedUser(RoutingContext routingContext) {
        io.gravitee.am.model.User user = routingContext.get("user");
        if (user != null) {
            User authenticatedUser = new DefaultUser(user.getUsername());
            Map<String, Object> additionalInformation = new HashMap<>(user.getAdditionalInformation());
            // add ip address and user agent
            additionalInformation.put("ipAddress", getClientIp(routingContext.request()));
            additionalInformation.put("userAgent", getUserAgent(routingContext.request()));
            ((DefaultUser) authenticatedUser).setAdditionalInformation(additionalInformation);
            return authenticatedUser;
        }
        return null;
    }

    private void doRedirect(HttpServerResponse response, String url) {
        response.putHeader(HttpHeaders.LOCATION, url).setStatusCode(302).end();
    }

    protected String getClientIp(HttpServerRequest request) {
        String remoteAddress = null;

        if (request != null) {
            remoteAddress = request.getHeader(io.gravitee.common.http.HttpHeaders.X_FORWARDED_HOST);
            if (remoteAddress == null || remoteAddress.isEmpty()) {
                remoteAddress = request.remoteAddress().host();
            }
        }
        return remoteAddress;
    }

    protected String getUserAgent(HttpServerRequest request) {
        return request.getHeader(io.gravitee.common.http.HttpHeaders.USER_AGENT);
    }
}
