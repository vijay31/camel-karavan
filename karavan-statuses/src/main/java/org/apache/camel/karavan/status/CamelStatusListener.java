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
package org.apache.camel.karavan.status;

import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.karavan.config.ConfigService;
import org.apache.camel.karavan.status.model.CamelStatus;
import org.apache.camel.karavan.status.model.CamelStatusRequest;
import org.apache.camel.karavan.status.model.CamelStatusValue;
import org.apache.camel.karavan.status.model.ContainerStatus;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.apache.camel.karavan.status.StatusEvents.CMD_COLLECT_CAMEL_STATUS;

@ApplicationScoped
public class CamelStatusListener {

    private static final Logger LOGGER = Logger.getLogger(CamelStatusListener.class.getName());

    @Inject
    StatusCache statusCache;

    @ConfigProperty(name = "karavan.environment")
    String environment;

    @Inject
    Vertx vertx;

    WebClient webClient;

    public WebClient getWebClient() {
        if (webClient == null) {
            webClient = WebClient.create(vertx);
        }
        return webClient;
    }

    @ConsumeEvent(value = CMD_COLLECT_CAMEL_STATUS, blocking = true, ordered = true)
    public void collectCamelStatuses(JsonObject data) {
        try {
            CamelStatusRequest dms = data.getJsonObject("camelStatusRequest").mapTo(CamelStatusRequest.class);
            ContainerStatus containerStatus = data.getJsonObject("containerStatus").mapTo(ContainerStatus.class);
            LOGGER.debug("Collect Camel Status for " + containerStatus.getContainerName());
            String projectId = dms.getProjectId();
            String containerName = dms.getContainerName();
            List<CamelStatusValue> statuses = new ArrayList<>();
            for (CamelStatusValue.Name statusName : CamelStatusValue.Name.values()) {
                String status = getCamelStatus(containerStatus, statusName);
                if (status != null) {
                    statuses.add(new CamelStatusValue(statusName, status));
                }
            }
            CamelStatus cs = new CamelStatus(projectId, containerName, statuses, environment);
            statusCache.saveCamelStatus(cs);
        } catch (Exception ex) {
            LOGGER.error("collectCamelStatuses " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
        }
    }

    public String getContainerAddressForStatus(ContainerStatus containerStatus) throws Exception {
        if (ConfigService.inKubernetes()) {
            return "http://" + containerStatus.getPodIP() + ":8080";
        } else if (ConfigService.inDocker()) {
            return "http://" + containerStatus.getContainerName() + ":8080";
        } else if (containerStatus.getPorts() != null && !containerStatus.getPorts().isEmpty()) {
            Integer port = containerStatus.getPorts().get(0).getPublicPort();
            if (port != null) {
                return "http://localhost:" + port;
            }
        }
        throw new Exception("No port configured for project " + containerStatus.getContainerName());
    }

    public String getCamelStatus(ContainerStatus containerStatus, CamelStatusValue.Name statusName) throws Exception {
        String url = getContainerAddressForStatus(containerStatus) + "/q/dev/" + statusName.name();
        try {
            return getResult(url, 500);
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.error("getCamelStatus " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
        }
        return null;
    }

    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 1000)
    public String getResult(String url, int timeout) throws InterruptedException, ExecutionException {
        try {
            HttpResponse<Buffer> result = getWebClient().getAbs(url).putHeader("Accept", "application/json")
                    .timeout(timeout).send().subscribeAsCompletionStage().toCompletableFuture().get();
            if (result.statusCode() == 200) {
                JsonObject res = result.bodyAsJsonObject();
                return res.encodePrettily();
            }
        } catch (Exception ex) {
            LOGGER.error("getResult " + url);
            LOGGER.error("getResult " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
        }
        return null;
    }
}