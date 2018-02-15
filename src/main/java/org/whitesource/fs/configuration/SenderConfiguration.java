/**
 * Copyright (C) 2014 WhiteSource Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.whitesource.fs.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;
import org.whitesource.agent.api.dispatch.UpdateType;
import org.whitesource.agent.client.ClientConstants;
import org.whitesource.fs.FSAConfiguration;

import java.util.Properties;

import static org.whitesource.agent.ConfigPropertyKeys.*;
import static org.whitesource.agent.ConfigPropertyKeys.PROXY_PASS_PROPERTY_KEY;
import static org.whitesource.agent.client.ClientConstants.CONNECTION_TIMEOUT_KEYWORD;
import static org.whitesource.agent.client.ClientConstants.SERVICE_URL_KEYWORD;

public class SenderConfiguration {

    private final boolean checkPolicies;
    private final String serviceUrl;
    private final String proxyHost;
    private final int connectionTimeOut;
    private final int proxyPort;
    private final String proxyUser;
    private final String proxyPassword;
    private final boolean forceCheckAllDependencies;
    private final boolean forceUpdate;
    private final String updateTypeValue;
    private final boolean enableImpactAnalysis;
    private final boolean ignoreCertificateCheck;
    private final int connectionRetries;
    private final int connectionRetriesIntervals;

    public SenderConfiguration(
            @JsonProperty(CHECK_POLICIES_PROPERTY_KEY) boolean checkPolicies,
            @JsonProperty(SERVICE_URL_KEYWORD) String serviceUrl,
            @JsonProperty(PROXY_HOST_PROPERTY_KEY) String proxyHost,
            @JsonProperty(CONNECTION_TIMEOUT_KEYWORD) int connectionTimeOut,
            @JsonProperty(PROXY_PORT_PROPERTY_KEY) int proxyPort,
            @JsonProperty(PROXY_USER_PROPERTY_KEY) String proxyUser,
            @JsonProperty(PROXY_PASS_PROPERTY_KEY) String proxyPassword,
            @JsonProperty(FORCE_CHECK_ALL_DEPENDENCIES) boolean forceCheckAllDependencies,
            @JsonProperty(FORCE_UPDATE) boolean forceUpdate,
            @JsonProperty(UPDATE_TYPE) String updateTypeValue,
            @JsonProperty(ENABLE_IMPACT_ANALYSIS) boolean enableImpactAnalysis,
            @JsonProperty(IGNORE_CERTIFICATE_CHECK) boolean ignoreCertificateCheck,
            @JsonProperty(CONNECTION_RETRIES) int connectionRetries,
            @JsonProperty(CONNECTION_RETRIES_INTERVALS) int connectionRetriesIntervals){
        this.checkPolicies = checkPolicies;
        this.serviceUrl = serviceUrl;
        this.proxyHost = proxyHost;
        this.connectionTimeOut = connectionTimeOut;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
        this.forceCheckAllDependencies = forceCheckAllDependencies;
        this.forceUpdate = forceUpdate;
        this.updateTypeValue = updateTypeValue;
        this.enableImpactAnalysis = enableImpactAnalysis;
        this.ignoreCertificateCheck = ignoreCertificateCheck;
        this.connectionRetries = connectionRetries;
        this.connectionRetriesIntervals = connectionRetriesIntervals;
    }

    public SenderConfiguration(Properties config) {

        updateTypeValue = config.getProperty(UPDATE_TYPE, UpdateType.OVERRIDE.toString());
        checkPolicies =  FSAConfiguration.getBooleanProperty(config, CHECK_POLICIES_PROPERTY_KEY, false);
        forceCheckAllDependencies = FSAConfiguration.getBooleanProperty(config, FORCE_CHECK_ALL_DEPENDENCIES, false);
        forceUpdate = FSAConfiguration.getBooleanProperty(config, FORCE_UPDATE, false);
        enableImpactAnalysis = FSAConfiguration.getBooleanProperty(config, ENABLE_IMPACT_ANALYSIS, false);
        serviceUrl = config.getProperty(SERVICE_URL_KEYWORD, ClientConstants.DEFAULT_SERVICE_URL);
        proxyHost = config.getProperty(PROXY_HOST_PROPERTY_KEY);
        connectionTimeOut = Integer.parseInt(config.getProperty(ClientConstants.CONNECTION_TIMEOUT_KEYWORD,
                String.valueOf(ClientConstants.DEFAULT_CONNECTION_TIMEOUT_MINUTES)));
        connectionRetries = FSAConfiguration.getIntProperty(config,CONNECTION_RETRIES,1);
        connectionRetriesIntervals = FSAConfiguration.getIntProperty(config,CONNECTION_RETRIES_INTERVALS,3000);
        String senderPort = config.getProperty(PROXY_PORT_PROPERTY_KEY);
        if(StringUtils.isNotEmpty(senderPort)){
            proxyPort = Integer.parseInt(senderPort);
        }else{
            proxyPort = -1;
        }

        proxyUser = config.getProperty(PROXY_USER_PROPERTY_KEY);
        proxyPassword = config.getProperty(PROXY_PASS_PROPERTY_KEY);
        ignoreCertificateCheck = FSAConfiguration.getBooleanProperty(config, IGNORE_CERTIFICATE_CHECK, false);
    }

    @JsonProperty(SERVICE_URL_KEYWORD)
    public String getServiceUrl() {
        return serviceUrl;
    }

    @JsonProperty(UPDATE_TYPE)
    public String getUpdateTypeValue() {
        return updateTypeValue;
    }

    @JsonProperty(CHECK_POLICIES_PROPERTY_KEY)
    public boolean isCheckPolicies() {
        return checkPolicies;
    }

    @JsonProperty(PROXY_HOST_PROPERTY_KEY)
    public String getProxyHost() {
        return proxyHost;
    }

    @JsonProperty(CONNECTION_TIMEOUT_KEYWORD)
    public int getConnectionTimeOut() {
        return connectionTimeOut;
    }

    @JsonProperty(CONNECTION_RETRIES)
    public int getConnectionRetries(){
        return connectionRetries;
    }

    @JsonProperty(CONNECTION_RETRIES_INTERVALS)
    public int getConnectionRetriesIntervals(){
        return connectionRetriesIntervals;
    }

    @JsonProperty(PROXY_PORT_PROPERTY_KEY)
    public int getProxyPort() {
        return proxyPort;
    }

    @JsonProperty(PROXY_USER_PROPERTY_KEY)
    public String getProxyUser() {
        return proxyUser;
    }

    @JsonProperty(PROXY_PASS_PROPERTY_KEY)
    public String getProxyPassword() {
        return proxyPassword;
    }

    @JsonProperty(FORCE_CHECK_ALL_DEPENDENCIES)
    public boolean isForceCheckAllDependencies() {
        return forceCheckAllDependencies;
    }

    @JsonProperty(FORCE_UPDATE)
    public boolean isForceUpdate() {
        return forceUpdate;
    }

    @JsonProperty(ENABLE_IMPACT_ANALYSIS)
    public boolean isEnableImpactAnalysis() {
        return enableImpactAnalysis;
    }

    @JsonProperty(IGNORE_CERTIFICATE_CHECK)
    public boolean isIgnoreCertificateCheck() {
        return ignoreCertificateCheck;
    }
}
