# Service Status Checker Authentication Action Plugin
[![Quality](https://img.shields.io/badge/quality-demo-red)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-source-blue)](https://curity.io/resources/code-examples/status/)

## Overview
This plugin is an authentication action for the Curity Identity Server that checks the availability of an external service by querying its specified endpoint. It caches the service status ("up" or "down") in a bucket with a configurable TTL (Time To Live) to optimize performance by reducing redundant checks.

## Features
- **Service Status Checking**: Validates the availability of an external service by sending an HTTP GET request to a configured URL.
- **Caching**: Stores the service status in a Curity bucket with a configurable TTL to minimize repeated HTTP requests.

## Installation

Download the release, and unzip it it to `$IDSVR_HOME/usr/share/plugins` on every node.

### Build plugin from source

1. Clone the repository:
   ```bash
   git clone https://github.com/Curity-PS/service-status-checker.git
   ```

2. Build the plugin using Gradle:
    ```bash
    cd service-status-checker
    ./gradlew jar
    ```

3. To install this plug-in to a local Curity Identity server, there is a convenience task that will copy the JAR and its dependencies to the correct location. Run the following command:
    ```bash
    IDSVR_HOME=/opt/idsvr/usr/share/plugins ./gradlew deployToLocal
    ```

4. Configure the plugin in the Curity admin interface (_see Configuration section_)

## Configuration
The plugin requires the following configuration parameters in the Curity Identity Server:

- **Service URL**: The URL of the external service to check (e.g., https://accounts.google.com/.well-known/openid-configuration).
- **Status Cache TTL**: The time (in seconds) for which the service status is cached in the bucket.
- **Http Client**: The HTTP client service provided by the Curity Identity Server SDK.
- **Cache Store Bucket**: The bucket service for caching the status.


Example Configuration : 

```xml
<config xmlns="http://tail-f.com/ns/config/1.0">
    <profiles xmlns="https://curity.se/ns/conf/base">
    <profile>
    <id>authentication</id>
    <type xmlns:auth="https://curity.se/ns/conf/profile/authentication">auth:authentication-service</type>
      <settings>
      <authentication-service xmlns="https://curity.se/ns/conf/profile/authentication">
      <authentication-actions>
      <authentication-action>
        <id>service-status-checker</id>
        <service-status-checker xmlns="https://curity.se/ns/ext-conf/service-status-checker">
          <cache-store-bucket>
            <data-source>DefaultHSQLDB</data-source>
          </cache-store-bucket>
          <http-client>
            <id>trustStoreHttpClient</id>
          </http-client>
          <service-url>https://accounts.google.com/.well-known/openid-configuration</service-url>
          <status-cache-ttl>60</status-cache-ttl>
        </service-status-checker>
      </authentication-action>
      </authentication-actions>
      </authentication-service>
      </settings>
  </profile>
  </profiles>
</config>
```

## Usage
The plugin checks the service status during the authentication process:
 1. **Cache Check**: If a valid cached status exists (within TTL), it returns the cached status ("up" or "down").
 2. **Service Check**: If no valid cache exists, it sends an HTTP GET request to the configured URL. If the response status code is 200, the service is considered "up". Otherwise, the service is marked "down".
 3. **Caching**: The status is cached in the bucket with the configured TTL.
 4. **Result**: The status is added as an attribute (service-status) to the authentication action result in action attributes.

## More Information
Please visit [curity.io](https://curity.io) for more information about the Curity Identity Server.
