/**
 * Copyright (C) 2017 WhiteSource Ltd.
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
package org.whitesource.agent.dependency.resolver;
import org.apache.commons.lang.StringUtils;
import org.whitesource.agent.api.model.DependencyType;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author eugen.horovitz
 */
public abstract class AbstractDependencyResolver {

    /* --- Static Members --- */

    private static final String BACK_SLASH = "\\";
    private static final String FORWARD_SLASH = "/";
    private static final String EMPTY_STRING = "";
    protected IBomParser bomParser;

    /* --- Abstract methods --- */

    protected abstract ResolutionResult resolveDependencies(String projectFolder, String topLevelFolder, Set<String> bomFiles);

    protected abstract Collection<String> getExcludes();

    protected abstract Collection<String> getSourceFileExtensions();

    protected abstract DependencyType getDependencyType();

    protected abstract String getBomPattern();

    protected abstract Collection<String> getLanguageExcludes();

    /* --- Protected methods --- */

    protected List<String> normalizeLocalPath(String parentFolder, String topFolderFound, Collection<String> excludes, String folderToIgnore) {
        String normalizedRoot = new File(parentFolder).getPath();
        if (normalizedRoot.equals(topFolderFound)) {
            topFolderFound = topFolderFound
                    .replace(normalizedRoot, EMPTY_STRING)
                    .replace(BACK_SLASH, FORWARD_SLASH);
        } else {
            topFolderFound = topFolderFound
                    .replace(parentFolder, EMPTY_STRING)
                    .replace(BACK_SLASH, FORWARD_SLASH);
        }

        if (topFolderFound.length() > 0)
            topFolderFound = topFolderFound.substring(1, topFolderFound.length()) + FORWARD_SLASH;

        String finalRes = topFolderFound;
        if (StringUtils.isBlank(folderToIgnore)) {
            return excludes.stream().map(exclude -> finalRes + exclude).collect(Collectors.toList());
        } else {
            return excludes.stream().map(exclude -> finalRes + folderToIgnore + FORWARD_SLASH + exclude).collect(Collectors.toList());
        }
    }
}