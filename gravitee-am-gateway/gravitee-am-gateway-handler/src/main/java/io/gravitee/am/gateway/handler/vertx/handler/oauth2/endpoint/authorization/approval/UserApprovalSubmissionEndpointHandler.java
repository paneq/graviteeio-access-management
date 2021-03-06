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
package io.gravitee.am.gateway.handler.vertx.handler.oauth2.endpoint.authorization.approval;

import io.gravitee.am.gateway.handler.oauth2.approval.ApprovalService;
import io.gravitee.am.gateway.handler.oauth2.exception.AccessDeniedException;
import io.gravitee.am.gateway.handler.oauth2.request.AuthorizationRequest;
import io.gravitee.am.gateway.handler.oauth2.utils.OAuth2Constants;
import io.gravitee.am.gateway.handler.vertx.utils.UriBuilderRequest;
import io.gravitee.am.model.Client;
import io.gravitee.am.model.Domain;
import io.gravitee.common.http.HttpHeaders;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * If the request is valid, the authorization server authenticates the resource owner and obtains
 * an authorization decision (by asking the resource owner or by establishing approval via other means).
 * When a decision is established, the authorization server directs the user-agent to the provided client redirection URI using an HTTP
 * redirection response, or by other means available to it via the user-agent.
 *
 * See <a href="https://tools.ietf.org/html/rfc6749#section-3.1">3.1. Authorization Endpoint</a>
 *
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserApprovalSubmissionEndpointHandler implements Handler<RoutingContext> {

    private static final Logger logger = LoggerFactory.getLogger(UserApprovalSubmissionEndpointHandler.class);
    private static final String CLIENT_CONTEXT_KEY = "client";
    private ApprovalService approvalService;
    private Domain domain;

    public UserApprovalSubmissionEndpointHandler(ApprovalService approvalService, Domain domain) {
        this.approvalService = approvalService;
        this.domain = domain;
    }

    @Override
    public void handle(RoutingContext context) {
        // retrieve client
        Client client = context.get(CLIENT_CONTEXT_KEY);

        // retrieve end user
        User authenticatedUser = context.user();
        io.gravitee.am.model.User endUser = ((io.gravitee.am.gateway.handler.vertx.auth.user.User) authenticatedUser.getDelegate()).getUser();

        // retrieve authorization request
        AuthorizationRequest authorizationRequest = context.session().get(OAuth2Constants.AUTHORIZATION_REQUEST);

        HttpServerRequest req = context.request();
        if (!req.isExpectMultipart()) {
            throw new IllegalStateException("Form body not parsed - do you forget to include a BodyHandler?");
        }

        // prepare user approval choices
        MultiMap params = req.formAttributes();

        // retrieve user approval choices
        Map<String, String> approvalParameters = params.getDelegate().entries()
                .stream()
                .filter(entry -> entry.getKey().startsWith(OAuth2Constants.SCOPE_PREFIX))
                .collect(Collectors.toMap(scopeEntry -> scopeEntry.getKey(), scopeEntry -> params.get(OAuth2Constants.USER_OAUTH_APPROVAL)));
        authorizationRequest.setApprovalParameters(approvalParameters);

        // handle approval response
        approvalService.saveApproval(authorizationRequest, client, endUser)
                .subscribe(authorizationRequest1 -> {
                    // user denied access
                    if (!authorizationRequest.isApproved()) {
                        context.fail(new AccessDeniedException("User denied access"));
                        return;
                    }

                    // user approved access, replay authorization request
                    final String authorizationRequestUrl = UriBuilderRequest.resolveProxyRequest(context.request(),"/" + domain.getPath() + "/oauth/authorize", authorizationRequest.getRequestParameters().toSingleValueMap());
                    doRedirect(context.response(), authorizationRequestUrl);
                },
                error -> {
                    logger.error("An error occurs while handling authorization approval request", error);
                    context.fail(error);
                });
        }

    private void doRedirect(HttpServerResponse response, String url) {
        response.putHeader(HttpHeaders.LOCATION, url).setStatusCode(302).end();
    }
}
