/**
 * synopsys-detect
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.detect.tool.detector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.detect.exception.DetectUserFriendlyException;
import com.synopsys.integration.detect.exitcode.ExitCodeType;
import com.synopsys.integration.detect.tool.detector.impl.ExtractionEnvironmentProvider;
import com.synopsys.integration.detect.workflow.event.Event;
import com.synopsys.integration.detect.workflow.event.EventSystem;
import com.synopsys.integration.detect.workflow.project.DetectorEvaluationNameVersionDecider;
import com.synopsys.integration.detect.workflow.project.DetectorNameVersionDecider;
import com.synopsys.integration.detect.workflow.report.util.DetectorEvaluationUtils;
import com.synopsys.integration.detect.workflow.status.DetectorStatus;
import com.synopsys.integration.detect.workflow.status.StatusType;
import com.synopsys.integration.detector.base.DetectorEvaluation;
import com.synopsys.integration.detector.base.DetectorEvaluationTree;
import com.synopsys.integration.detector.base.DetectorType;
import com.synopsys.integration.detector.evaluation.DetectorEvaluator;
import com.synopsys.integration.detector.finder.DetectorFinder;
import com.synopsys.integration.detector.finder.DetectorFinderDirectoryListException;
import com.synopsys.integration.detector.finder.DetectorFinderOptions;
import com.synopsys.integration.detector.rule.DetectorRule;
import com.synopsys.integration.detector.rule.DetectorRuleSet;
import com.synopsys.integration.util.NameVersion;
import java.io.File;
import java.util.stream.Collectors;

public class DetectorTool {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExtractionEnvironmentProvider extractionEnvironmentProvider;
    private final ExternalIdFactory externalIdFactory = new ExternalIdFactory();
    private final DetectableFactory detectableFactory;
    private final EventSystem eventSystem;
    private final CodeLocationConverter codeLocationConverter;

    public DetectorTool(ExtractionEnvironmentProvider extractionEnvironmentProvider, final DetectableFactory detectableFactory, final EventSystem eventSystem,
        final CodeLocationConverter codeLocationConverter) {
        this.extractionEnvironmentProvider = extractionEnvironmentProvider;
        this.detectableFactory = detectableFactory;
        this.eventSystem = eventSystem;
        this.codeLocationConverter = codeLocationConverter;
    }

    public DetectorToolResult performDetectors(File directory, DetectorFinderOptions detectorFinderOptions, String projectBomTool) throws DetectUserFriendlyException {
        logger.info("Initializing detector system.");

        DetectorFinder detectorFinder = new DetectorFinder();
        DetectorRuleFactory detectorRuleFactory = new DetectorRuleFactory();
        DetectorRuleSet detectRuleSet = detectorRuleFactory.createRules(detectableFactory, false);//TODO add the parse flag

        Optional<DetectorEvaluationTree> possibleRootEvaluation;
        try {
            logger.info("Starting detector file system traversal.");
            possibleRootEvaluation = detectorFinder.findDetectors(directory, detectRuleSet, detectorFinderOptions);

        } catch (DetectorFinderDirectoryListException e) {
            throw new DetectUserFriendlyException("Detect was unable to list a directory while searching for detectors.", e, ExitCodeType.FAILURE_DETECTOR);
        }

        if (!possibleRootEvaluation.isPresent()){
            return new DetectorToolResult();
        }

        DetectorEvaluationTree rootEvaluation = possibleRootEvaluation.get();
        List<DetectorEvaluation> detectorEvaluations = DetectorEvaluationUtils.flatten(rootEvaluation);

        logger.trace("Setting up detector events.");
        DetectorEventBroadcaster eventBroadcaster = new DetectorEventBroadcaster(eventSystem);
        DetectorEvaluator detectorEvaluator = new DetectorEvaluator();
        detectorEvaluator.setDetectorEventListener(eventBroadcaster);

        logger.info("Starting detector search.");
        detectorEvaluator.searchAndApplicableEvaluation(rootEvaluation, new HashSet<>());
        eventSystem.publishEvent(Event.SearchCompleted, rootEvaluation);

        logger.info("Starting detector preparation.");
        detectorEvaluator.extractableEvaluation(rootEvaluation);
        eventSystem.publishEvent(Event.PreparationsCompleted, rootEvaluation);

        logger.info("Starting detector extraction.");
        Integer extractionCount = Math.toIntExact(detectorEvaluations.stream()
                                  .filter(DetectorEvaluation::isExtractable)
                                  .count());
        eventSystem.publishEvent(Event.ExtractionCount, extractionCount);

        logger.info("Total number of extractions: " + extractionCount);

        detectorEvaluator.extractionEvaluation(rootEvaluation, extractionEnvironmentProvider::createExtractionEnvironment);
        eventSystem.publishEvent(Event.ExtractionsCompleted, rootEvaluation);

        Map<DetectorType, StatusType> statusMap = extractStatus(detectorEvaluations);
        statusMap.forEach((detectorType, statusType) -> eventSystem.publishEvent(Event.StatusSummary, new DetectorStatus(detectorType, statusType)));

        DetectorToolResult detectorToolResult = new DetectorToolResult();

        detectorToolResult.rootDetectorEvaluationTree = rootEvaluation;

        detectorToolResult.applicableDetectorTypes = detectorEvaluations.stream()
                                                         .filter(DetectorEvaluation::isApplicable)
                                                         .map(DetectorEvaluation::getDetectorRule)
                                                         .map(DetectorRule::getDetectorType)
                                                         .collect(Collectors.toSet());

        detectorToolResult.codeLocationMap = detectorEvaluations.stream()
                                                      .filter(DetectorEvaluation::wasExtractionSuccessful)
                                                      .map(it -> codeLocationConverter.toDetectCodeLocation(directory, it))
                                                      .map(Map::entrySet)
                                                      .flatMap(Collection::stream)
                                                      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        detectorToolResult.bomToolCodeLocations = new ArrayList<>(detectorToolResult.codeLocationMap.values());

        DetectorEvaluationNameVersionDecider detectorEvaluationNameVersionDecider = new DetectorEvaluationNameVersionDecider(new DetectorNameVersionDecider());
        Optional<NameVersion> bomToolNameVersion = detectorEvaluationNameVersionDecider.decideSuggestion(detectorEvaluations, projectBomTool);
        detectorToolResult.bomToolProjectNameVersion = bomToolNameVersion;
        logger.info("Finished evaluating detectors for project info.");

        //Completed.
        logger.info("Finished running detectors.");
        eventSystem.publishEvent(Event.DetectorsComplete, detectorToolResult);

        return detectorToolResult;
    }

    private  Map<DetectorType, StatusType> extractStatus(List<DetectorEvaluation> detectorEvaluations) {
        Map<DetectorType, StatusType> statusMap = new HashMap<>();
        for (DetectorEvaluation detectorEvaluation : detectorEvaluations) {
            DetectorType detectorType = detectorEvaluation.getDetectorRule().getDetectorType();
            if (detectorEvaluation.isApplicable()){
                StatusType statusType;
                if (detectorEvaluation.isExtractable()){
                    if (detectorEvaluation.wasExtractionSuccessful()){
                        statusType = StatusType.SUCCESS;
                    } else if (detectorEvaluation.wasExtractionFailure()){
                        statusType = StatusType.FAILURE;
                    } else if (detectorEvaluation.wasExtractionException()) {
                        statusType = StatusType.FAILURE;
                    } else {
                        logger.warn("An issue occured in the detector system, an unknown evaluation status was created. Please don't do this again.");
                        statusType = StatusType.FAILURE;
                    }
                } else {
                    statusType = StatusType.FAILURE;
                }
                statusMap.put(detectorType, statusType);
            }
        }
        return statusMap;
    }
}
