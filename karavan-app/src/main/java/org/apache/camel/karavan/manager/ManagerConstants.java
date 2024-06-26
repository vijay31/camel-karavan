/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.karavan.manager;

public class ManagerConstants {

    public static final String DEV_ENV = "dev";
    public static final String ENV_VAR_JBANG_OPTIONS = "JBANG_OPTIONS";

    public static final String LABEL_PART_OF = "app.kubernetes.io/part-of";
    public static final String LABEL_TYPE = "org.apache.camel.karavan/type";
    public static final String LABEL_PROJECT_ID = "org.apache.camel.karavan/projectId";
    public static final String LABEL_CAMEL_RUNTIME = "org.apache.camel.karavan/runtime";
    public static final String LABEL_TAG = "org.apache.camel.karavan/tag";

    public static final String BUILDER_SUFFIX = "-builder";

    public static final String CAMEL_PREFIX = "camel";
    public static final String KARAVAN_PREFIX = "karavan";

    public static final String BUILD_CONFIG_MAP = "build-config-map";
    public static final String BUILD_DOCKER_CONFIG_SECRET = "dockerconfigjson";
    public static final String PRIVATE_KEY_SECRET_KEY = "private-key";
    public static final String KNOWN_HOSTS_SECRET_KEY = "known-hosts";
}
