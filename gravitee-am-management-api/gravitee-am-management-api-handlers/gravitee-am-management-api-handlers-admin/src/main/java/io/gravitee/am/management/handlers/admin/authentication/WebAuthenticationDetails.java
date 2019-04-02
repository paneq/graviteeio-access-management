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

import io.gravitee.common.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WebAuthenticationDetails implements Serializable {

    private final String remoteAddress;
    private final String sessionId;
    private final String userAgent;

    /**
     * Records the remote address and will also set the session Id if a session already
     * exists (it won't create one).
     *
     * @param request that the authentication request was received from
     */
    public WebAuthenticationDetails(HttpServletRequest request) {
        this.remoteAddress = getClientIp(request);

        HttpSession session = request.getSession(false);
        this.sessionId = (session != null) ? session.getId() : null;

        userAgent = request.getHeader(HttpHeaders.USER_AGENT);
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(": ");
        sb.append("RemoteIpAddress: ").append(this.getRemoteAddress()).append("; ");
        sb.append("SessionId: ").append(this.getSessionId());
        sb.append("UserAgent: ").append(this.getUserAgent());

        return sb.toString();
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
}
