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

import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.config.annotation.DefaultInteger;
import se.curity.identityserver.sdk.config.annotation.DefaultService;
import se.curity.identityserver.sdk.config.annotation.Description;
import se.curity.identityserver.sdk.service.Bucket;
import se.curity.identityserver.sdk.service.HttpClient;

public interface ServiceStatusCheckerAuthenticationActionConfig extends Configuration
{
    @Description("The URL of the external service to check")
    String getServiceUrl();

    @Description("Status Cache Time to Live, in seconds.")
    @DefaultInteger(60)
    int getStatusCacheTTL();

    @Description("Bucket data store for caching the service status")
    Bucket getCacheStoreBucket();

    @DefaultService
    HttpClient getHttpClient();
}
