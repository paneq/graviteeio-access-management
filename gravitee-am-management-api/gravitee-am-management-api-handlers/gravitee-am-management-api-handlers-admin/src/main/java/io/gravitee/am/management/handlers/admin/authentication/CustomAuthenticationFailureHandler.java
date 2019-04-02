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
package io.gravitee.am.management.handlers.admin.authentication;

import io.gravitee.am.identityprovider.api.Authentication;
import io.gravitee.am.management.handlers.admin.provider.security.EndUserAuthentication;
import io.gravitee.am.model.Domain;
import io.gravitee.am.service.AuditService;
import io.gravitee.am.service.reporter.builder.AuditBuilder;
import io.gravitee.am.service.reporter.builder.AuthenticationAuditBuilder;
import io.gravitee.common.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final String CLIENT_ID = "admin";

    @Autowired
    private AuditService auditService;

    @Autowired
    private Domain domain;

    public CustomAuthenticationFailureHandler() {
        super();
    }

    public CustomAuthenticationFailureHandler(String defaultFailureUrl) {
        super(defaultFailureUrl);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        Authentication authentication = new EndUserAuthentication(request.getParameter("username"), null);
        Map<String, Object> additionalInformation = new HashMap();
        additionalInformation.put("ipAddress", getClientIp(request));
        additionalInformation.put("userAgent", getUserAgent(request));
        ((EndUserAuthentication) authentication).setAdditionalInformation(additionalInformation);

        // audit event
        auditService.report(AuditBuilder.builder(AuthenticationAuditBuilder.class).principal(authentication).domain(domain.getId()).client(CLIENT_ID).throwable(exception));

        super.onAuthenticationFailure(request, response, exception);
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddress = null;

        if (request != null) {
            remoteAddress = request.getHeader(HttpHeaders.X_FORWARDED_HOST);
            if (remoteAddress == null || remoteAddress.isEmpty()) {
                remoteAddress = request.getRemoteAddr();
            }
        }
        return remoteAddress;
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.USER_AGENT);
    }
}
