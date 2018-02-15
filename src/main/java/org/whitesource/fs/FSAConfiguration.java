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
package org.whitesource.fs;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;
import org.whitesource.agent.ConfigPropertyKeys;
import org.whitesource.agent.utils.Pair;
import org.whitesource.fs.configuration.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static org.whitesource.agent.ConfigPropertyKeys.*;

/**
 * Author: eugen.horovitz
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FSAConfiguration {

    /* --- Static members --- */

    public static Collection<String> ignoredWebProperties = Arrays.asList(
            SCM_REPOSITORIES_FILE, LOG_LEVEL_KEY, FOLLOW_SYMBOLIC_LINKS, SHOW_PROGRESS_BAR, PROJECT_CONFIGURATION_PATH, SCAN_PACKAGE_MANAGER, WHITESOURCE_FOLDER_PATH,
            ENDPOINT_ENABLED, ENDPOINT_PORT, ENDPOINT_CERTIFICATE, ENDPOINT_PASS, ENDPOINT_SSL_ENABLED, OFFLINE_PROPERTY_KEY, OFFLINE_ZIP_PROPERTY_KEY, OFFLINE_PRETTY_JSON_KEY, WHITESOURCE_CONFIGURATION);

    private static final String FALSE = "false";
    private static final String INFO = "info";
    public static final String INCLUDES_EXCLUDES_SEPARATOR_REGEX = "[,;\\s]+";
    private static final int DEFAULT_ARCHIVE_DEPTH = 0;
    private static final String NONE = "(none)";
    private static final String SPACE = " ";
    private static final String BLANK = "";

    /* --- Private fields --- */

    private final ScmConfiguration scm;
    private final SenderConfiguration sender;
    private final OfflineConfiguration offline;
    private final ResolverConfiguration resolver;
    private final ConfigurationValidation configurationValidation;
    private final EndPointConfiguration endpoint;

    private final List<String> errors;

    /* --- Private final fields --- */

    private final List<String> offlineRequestFiles;
    private final String fileListPath;
    private final List<String> dependencyDirs;
    private final String configFilePath;
    private final AgentConfiguration agent;
    private final RequestConfiguration request;
    private final boolean scanPackageManager;

    private String logLevel;
    private boolean useCommandLineProductName;
    private boolean useCommandLineProjectName;

    /* --- Constructors --- */

    public FSAConfiguration(Properties config) {
        this(config, null);
    }

    public FSAConfiguration() {
        this(new Properties(), null);
    }

    public FSAConfiguration(String[] args) {
        this(null, args);
    }

    public FSAConfiguration(Properties config, String[] args) {
        configurationValidation = new ConfigurationValidation();
        String projectName;
        errors = new ArrayList<>();
        if ((args != null)) {
            // read command line args
            // validate args // TODO use jCommander validators
            // TODO add usage command
            CommandLineArgs commandLineArgs = new CommandLineArgs();
            new JCommander(commandLineArgs, args);

            if (config == null) {
                Pair<Properties, List<String>> propertiesWithErrors = readWithError(commandLineArgs.configFilePath);
                errors.addAll(propertiesWithErrors.getValue());
                config = propertiesWithErrors.getKey();
                if (StringUtils.isNotEmpty(commandLineArgs.project)) {
                    config.setProperty(PROJECT_NAME_PROPERTY_KEY, commandLineArgs.project);
                }
            }

            configFilePath = commandLineArgs.configFilePath;
            config.setProperty(PROJECT_CONFIGURATION_PATH, commandLineArgs.configFilePath);

            //override
            offlineRequestFiles = updateProperties(config, commandLineArgs);
            projectName = config.getProperty(PROJECT_NAME_PROPERTY_KEY);
            fileListPath = commandLineArgs.fileListPath;
            dependencyDirs = commandLineArgs.dependencyDirs;
            if (StringUtils.isNotBlank(commandLineArgs.whiteSourceFolder)) {
                config.setProperty(WHITESOURCE_FOLDER_PATH, commandLineArgs.whiteSourceFolder);
            }
            commandLineArgsOverride(commandLineArgs);
        } else {
            projectName = config.getProperty(PROJECT_NAME_PROPERTY_KEY);
            configFilePath = NONE;
            offlineRequestFiles = new ArrayList<>();
            fileListPath = null;
            dependencyDirs = new ArrayList<>();
            commandLineArgsOverride(null);
        }

        scanPackageManager = getBooleanProperty(config, SCAN_PACKAGE_MANAGER, false);

        // validate config
        String projectToken = config.getProperty(PROJECT_TOKEN_PROPERTY_KEY);
        String projectNameFinal = !StringUtils.isBlank(projectName) ? projectName : config.getProperty(PROJECT_NAME_PROPERTY_KEY);
        boolean projectPerFolder = FSAConfiguration.getBooleanProperty(config, PROJECT_PER_SUBFOLDER, false);
        String apiToken = config.getProperty(ORG_TOKEN_PROPERTY_KEY);
        int archiveExtractionDepth = FSAConfiguration.getArchiveDepth(config);
        String[] includes = FSAConfiguration.getIncludes(config);

        // todo: check possibility to get the errors only in the end
        errors.addAll(configurationValidation.getConfigurationErrors(projectPerFolder, projectToken, projectNameFinal, apiToken, configFilePath, archiveExtractionDepth, includes));

        logLevel = config.getProperty(LOG_LEVEL_KEY, INFO);

        String productToken = config.getProperty(ConfigPropertyKeys.PRODUCT_TOKEN_PROPERTY_KEY);
        String productName = config.getProperty(ConfigPropertyKeys.PRODUCT_NAME_PROPERTY_KEY);
        String productVersion = config.getProperty(ConfigPropertyKeys.PRODUCT_VERSION_PROPERTY_KEY);
        String projectVersion = config.getProperty(PROJECT_VERSION_PROPERTY_KEY);
        String appPath = config.getProperty(APP_PATH, BLANK);
        boolean projectPerSubFolder = getBooleanProperty(config, PROJECT_PER_SUBFOLDER, false);
        String requesterEmail = config.getProperty(REQUESTER_EMAIL);

        request = new RequestConfiguration(apiToken, requesterEmail, projectPerSubFolder, projectName, projectToken, projectVersion, productName, productToken, productVersion, appPath);
        scm = new ScmConfiguration(config);
        agent = new AgentConfiguration(config);
        offline = new OfflineConfiguration(config);
        sender = new SenderConfiguration(config);
        resolver = new ResolverConfiguration(config);
        endpoint = new EndPointConfiguration(config);
    }

    public static Pair<Properties, List<String>> readWithError(String configFilePath) {
        Properties configProps = new Properties();
        List<String> errors = new ArrayList<>();
        try {
            try (FileInputStream inputStream = new FileInputStream(configFilePath)) {
                try {
                    configProps.load(inputStream);
                } catch (FileNotFoundException e) {
                    errors.add("Failed to open " + configFilePath + " for reading " + e);
                } catch (IOException e) {
                    errors.add("Error occurred when reading from " + configFilePath + e);
                }
            }
        } catch (IOException e) {
            errors.add("Error occurred when reading from " + configFilePath + " - " + e);
        }
        return new Pair<>(configProps, errors);
    }

    /* --- Public getters --- */

    public RequestConfiguration getRequest() {
        return request;
    }

    public EndPointConfiguration getEndpoint() {
        return endpoint;
    }

    public SenderConfiguration getSender() {
        return sender;
    }

    public ScmConfiguration getScm() {
        return scm;
    }

    public AgentConfiguration getAgent() {
        return agent;
    }

    public OfflineConfiguration getOffline() {
        return offline;
    }

    public ResolverConfiguration getResolver() {
        return resolver;
    }

    List<String> getErrors() {
        return errors;
    }

    public List<String> getOfflineRequestFiles() {
        return offlineRequestFiles;
    }

    public String getFileListPath() {
        return fileListPath;
    }

    public List<String> getDependencyDirs() {
        return dependencyDirs;
    }

    public boolean getUseCommandLineProductName(){
        return useCommandLineProductName;
    }

    public boolean getUseCommandLineProjectName(){
        return useCommandLineProjectName;
    }

    @JsonProperty(SCAN_PACKAGE_MANAGER)
    public boolean isScanProjectManager() {
        return scanPackageManager;
    }

    @JsonProperty(LOG_LEVEL_KEY)
    public String getLogLevel() {
        return logLevel;
    }


    /* --- Public static methods--- */

    public static int getIntProperty(Properties config, String propertyKey, int defaultValue) {
        int value = defaultValue;
        String propertyValue = config.getProperty(propertyKey);
        if (StringUtils.isNotBlank(propertyValue)) {
            try {
                value = Integer.valueOf(propertyValue);
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        return value;
    }

    public static boolean getBooleanProperty(Properties config, String propertyKey, boolean defaultValue) {
        boolean property = defaultValue;
        String propertyValue = config.getProperty(propertyKey);
        if (StringUtils.isNotBlank(propertyValue)) {
            property = Boolean.valueOf(propertyValue);
        }
        return property;
    }

    public static long getLongProperty(Properties config, String propertyKey, long defaultValue) {
        long property = defaultValue;
        String propertyValue = config.getProperty(propertyKey);
        if (StringUtils.isNotBlank(propertyValue)) {
            property = Long.parseLong(propertyValue);
        }
        return property;
    }

    public static String[] getListProperty(Properties config, String propertyName, String[] defaultValue) {
        String property = config.getProperty(propertyName);
        if (property == null) {
            return defaultValue;
        }
        return property.split(SPACE);
    }

    public static int getArchiveDepth(Properties configProps) {
        return getIntProperty(configProps, ARCHIVE_EXTRACTION_DEPTH_KEY, FSAConfiguration.DEFAULT_ARCHIVE_DEPTH);
    }

    public static String[] getIncludes(Properties configProps) {
        String includesString = configProps.getProperty(INCLUDES_PATTERN_PROPERTY_KEY, "");
        if (StringUtils.isNotBlank(includesString)) {
            return configProps.getProperty(INCLUDES_PATTERN_PROPERTY_KEY, "").split(FSAConfiguration.INCLUDES_EXCLUDES_SEPARATOR_REGEX);
        }
        return new String[0];
    }

    /* --- Private methods --- */

    private List<String> updateProperties(Properties configProps, CommandLineArgs commandLineArgs) {
        // Check whether the user inserted api key, project OR/AND product via command line
        readPropertyFromCommandLine(configProps, ConfigPropertyKeys.ORG_TOKEN_PROPERTY_KEY, commandLineArgs.apiKey);
        readPropertyFromCommandLine(configProps, ConfigPropertyKeys.UPDATE_TYPE, commandLineArgs.updateType);
        readPropertyFromCommandLine(configProps, ConfigPropertyKeys.PRODUCT_NAME_PROPERTY_KEY, commandLineArgs.product);
        readPropertyFromCommandLine(configProps, ConfigPropertyKeys.PRODUCT_VERSION_PROPERTY_KEY, commandLineArgs.productVersion);
        readPropertyFromCommandLine(configProps, ConfigPropertyKeys.PROJECT_VERSION_PROPERTY_KEY, commandLineArgs.projectVersion);

        // request file
        List<String> offlineRequestFiles = new LinkedList<>();
        offlineRequestFiles.addAll(commandLineArgs.requestFiles);
        if (offlineRequestFiles.size() > 0) {
            configProps.put(ConfigPropertyKeys.OFFLINE_PROPERTY_KEY, FALSE);
        }
        readPropertyFromCommandLine(configProps, ConfigPropertyKeys.OFFLINE_PROPERTY_KEY, commandLineArgs.offline);
        //Impact Analysis parameters
        readPropertyFromCommandLine(configProps, ConfigPropertyKeys.APP_PATH, commandLineArgs.appPath);
        readPropertyFromCommandLine(configProps, ConfigPropertyKeys.ENABLE_IMPACT_ANALYSIS, commandLineArgs.enableImpactAnalysis);
        // proxy
        readPropertyFromCommandLine(configProps, ConfigPropertyKeys.PROXY_HOST_PROPERTY_KEY, commandLineArgs.proxyHost);
        readPropertyFromCommandLine(configProps, ConfigPropertyKeys.PROXY_PORT_PROPERTY_KEY, commandLineArgs.proxyPort);
        readPropertyFromCommandLine(configProps, ConfigPropertyKeys.PROXY_USER_PROPERTY_KEY, commandLineArgs.proxyUser);
        readPropertyFromCommandLine(configProps, ConfigPropertyKeys.PROXY_PASS_PROPERTY_KEY, commandLineArgs.proxyPass);

        // archiving
        readPropertyFromCommandLine(configProps, ConfigPropertyKeys.ARCHIVE_FAST_UNPACK_KEY, commandLineArgs.archiveFastUnpack);

        // project per folder
        readPropertyFromCommandLine(configProps, ConfigPropertyKeys.PROJECT_PER_SUBFOLDER, commandLineArgs.projectPerFolder);

        // Check whether the user inserted scmRepositoriesFile via command line
        readPropertyFromCommandLine(configProps, ConfigPropertyKeys.SCM_REPOSITORIES_FILE, commandLineArgs.repositoriesFile);

        return offlineRequestFiles;
    }

    private void readPropertyFromCommandLine(Properties configProps, String propertyKey, String propertyValue) {
        if (StringUtils.isNotBlank(propertyValue)) {
            configProps.put(propertyKey, propertyValue);
        }
    }

    private void commandLineArgsOverride(CommandLineArgs commandLineArgs){
        useCommandLineProductName = commandLineArgs != null && StringUtils.isNotBlank(commandLineArgs.product);
        useCommandLineProjectName = commandLineArgs != null && StringUtils.isNotBlank(commandLineArgs.project);
    }

    public void validate() {
        getErrors().clear();
        errors.addAll(configurationValidation.getConfigurationErrors(getRequest().isProjectPerSubFolder(),getRequest().getProjectToken(),
                getRequest().getProjectName(), getRequest().getApiToken(), configFilePath, getAgent().getArchiveExtractionDepth(), getAgent().getIncludes()));
    }
}