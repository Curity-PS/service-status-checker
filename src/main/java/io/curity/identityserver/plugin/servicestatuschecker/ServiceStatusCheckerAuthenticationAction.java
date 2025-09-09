/*
 *  Copyright 2025 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.identityserver.plugin.servicestatuschecker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.attribute.AuthenticationActionAttributes;
import se.curity.identityserver.sdk.authenticationaction.AuthenticationAction;
import se.curity.identityserver.sdk.authenticationaction.AuthenticationActionContext;
import se.curity.identityserver.sdk.authenticationaction.AuthenticationActionResult;
import se.curity.identityserver.sdk.errors.ErrorCode;
import se.curity.identityserver.sdk.http.HttpResponse;
import se.curity.identityserver.sdk.service.Bucket;
import se.curity.identityserver.sdk.service.HttpClient;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

public final class ServiceStatusCheckerAuthenticationAction implements AuthenticationAction
{
    private static final Logger logger = LoggerFactory.getLogger(ServiceStatusCheckerAuthenticationAction.class);
    private static final String BUCKET_KEY = "service-status";
    private static final String BUCKET_PURPOSE = "status-cache";
    private static final String STATUS_KEY = "status";
    private static final String TTL_KEY = "ttl";
    private static final String STATUS_UP = "up";
    private static final String STATUS_DOWN = "down";

    private final ServiceStatusCheckerAuthenticationActionConfig _config;
    private final HttpClient _httpClient;
    private final Bucket _bucket;

    public ServiceStatusCheckerAuthenticationAction(ServiceStatusCheckerAuthenticationActionConfig configuration)
    {
        this._config = configuration;
        this._httpClient = configuration.getHttpClient();
        this._bucket = configuration.getCacheStoreBucket();
    }

    @Override
    public AuthenticationActionResult apply(AuthenticationActionContext context)
    {
        String serviceUrl = _config.getServiceUrl();
        if (!isValidUrl(serviceUrl))
        {
            logger.debug("Service URL is not valid -> {}", serviceUrl);
            return AuthenticationActionResult.failedResult("Service URL is not valid", ErrorCode.CONFIGURATION_ERROR);
        }

        long ttlMillis = _config.getStatusCacheTTL() * 1000L;
        AuthenticationActionAttributes actionAttributes = context.getActionAttributes();

        // Check cached status
        Map<String, Object> cachedAttributes = _bucket.getAttributes(BUCKET_KEY, BUCKET_PURPOSE);
        if (isValidCachedStatus(cachedAttributes))
        {
            String status = (String) cachedAttributes.get(STATUS_KEY);
            logger.debug("Cache-hit: Using cached service status: {}", status);
            return createResult(context, actionAttributes, status);
        }

        // Perform service status check
        String status = checkServiceStatus(serviceUrl);
        // Cache the result
        _bucket.storeAttributes(BUCKET_KEY, BUCKET_PURPOSE, Map.of(
                STATUS_KEY, status,
                TTL_KEY, System.currentTimeMillis() + ttlMillis
        ));

        logger.debug("Cache-miss: Storing service status in bucket cache : {} with TTL {}ms", status, ttlMillis);

        return createResult(context, actionAttributes, status);
    }

    private boolean isValidUrl(String urlString)
    {
        try
        {
            URI.create(urlString).toURL();
            return true;
        }
        catch (IllegalArgumentException | MalformedURLException e)
        {
            return false;
        }
    }

    private boolean isValidCachedStatus(Map<String, Object> attributes)
    {
        if (!attributes.containsKey(STATUS_KEY) || !attributes.containsKey(TTL_KEY))
        {
            return false;
        }
        Long ttl = (Long) attributes.get(TTL_KEY);
        return ttl != null && System.currentTimeMillis() < ttl;
    }

    private String checkServiceStatus(String serviceUrl)
    {
        try
        {
            HttpResponse response = _httpClient.request(URI.create(serviceUrl))
                    .get()
                    .response();

            return response.statusCode() == 200 ? STATUS_UP : STATUS_DOWN;
        }
        catch (Exception e)
        {
            logger.debug("Failed to check service status: {}", e.getMessage());
            return STATUS_DOWN;
        }
    }

    private AuthenticationActionResult createResult(
            AuthenticationActionContext context,
            AuthenticationActionAttributes actionAttributes,
            String status
    )
    {
        AuthenticationActionAttributes updatedActionAttributes =
                actionAttributes.with(Attribute.of(BUCKET_KEY, status));
        return AuthenticationActionResult.successfulResult(
                context.getAuthenticationAttributes(),
                updatedActionAttributes
        );
    }
}
