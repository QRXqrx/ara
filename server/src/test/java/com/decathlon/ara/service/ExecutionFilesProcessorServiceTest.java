/******************************************************************************
 * Copyright (C) 2020 by the ARA Contributors                                 *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 * 	 http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 *                                                                            *
 ******************************************************************************/

package com.decathlon.ara.service;

import com.decathlon.ara.ci.bean.*;
import com.decathlon.ara.ci.service.QualityService;
import com.decathlon.ara.ci.util.JsonParserConsumer;
import com.decathlon.ara.domain.*;
import com.decathlon.ara.domain.enumeration.*;
import com.decathlon.ara.repository.*;
import com.decathlon.ara.test.strategy.ScenariosIndexerStrategy;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExecutionFilesProcessorServiceTest {

    private final static String BUILD_INFORMATION_FILE_NAME = "buildInformation.json";

    private final static String CYCLE_DEFINITION_FILE_NAME = "cycleDefinition.json";

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SettingService settingService;

    @Mock
    private SettingProviderService settingProviderService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private JsonParser jsonParser;

    @Mock
    private JsonParserConsumer jsonParserConsumer;

    @Mock
    private ExecutionCompletionRequestRepository executionCompletionRequestRepository;

    @Mock
    private ExecutionRepository executionRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private TypeRepository typeRepository;

    @Mock
    private QualityService qualityService;

    @Mock
    private ScenariosIndexerStrategy scenariosIndexerStrategy; 

    @InjectMocks
    private ExecutionFilesProcessorService cut;

    @Test
    public void getExecution_returnEmpty_whenPlannedIndexationIsNull(){
        // Given

        // When

        // Then
        Optional<Execution> execution = cut.getExecution(null);
        assertThat(execution).isEmpty();
        verify(executionCompletionRequestRepository, never()).delete(any(ExecutionCompletionRequest.class));
        verify(qualityService, never()).computeQuality(any(Execution.class));
        verify(scenariosIndexerStrategy, never()).getScenariosIndexer(any(Technology.class));
    }

    @Test
    public void getExecution_returnEmpty_whenExecutionFolderIsNull(){
        // Given
        PlannedIndexation plannedIndexation = mock(PlannedIndexation.class);

        // When
        when(plannedIndexation.getExecutionFolder()).thenReturn(null);

        // Then
        Optional<Execution> execution = cut.getExecution(plannedIndexation);
        assertThat(execution).isEmpty();
        verify(executionCompletionRequestRepository, never()).delete(any(ExecutionCompletionRequest.class));
        verify(qualityService, never()).computeQuality(any(Execution.class));
        verify(scenariosIndexerStrategy, never()).getScenariosIndexer(any(Technology.class));
    }

    @Test
    public void getExecution_returnEmpty_whenCycleDefinitionIsNull(){
        // Given
        PlannedIndexation plannedIndexation = mock(PlannedIndexation.class);
        File executionFile = mock(File.class);

        // When
        when(plannedIndexation.getExecutionFolder()).thenReturn(executionFile);
        when(plannedIndexation.getCycleDefinition()).thenReturn(null);

        // Then
        Optional<Execution> execution = cut.getExecution(plannedIndexation);
        assertThat(execution).isEmpty();
        verify(executionCompletionRequestRepository, never()).delete(any(ExecutionCompletionRequest.class));
        verify(qualityService, never()).computeQuality(any(Execution.class));
        verify(scenariosIndexerStrategy, never()).getScenariosIndexer(any(Technology.class));
    }

    @Test
    public void getExecution_returnEmpty_whenExecutionFolderIsEmpty(){
        // Given
        PlannedIndexation plannedIndexation = mock(PlannedIndexation.class);
        File executionFile = mock(File.class);
        CycleDefinition cycleDefinition = mock(CycleDefinition.class);

        // When
        when(plannedIndexation.getExecutionFolder()).thenReturn(executionFile);
        when(executionFile.getAbsolutePath()).thenReturn("/execution/absolute/path");
        when(executionFile.listFiles()).thenReturn(new File[0]);
        when(plannedIndexation.getCycleDefinition()).thenReturn(cycleDefinition);

        // Then
        Optional<Execution> execution = cut.getExecution(plannedIndexation);
        assertThat(execution).isEmpty();
        verify(executionCompletionRequestRepository, never()).delete(any(ExecutionCompletionRequest.class));
        verify(qualityService, never()).computeQuality(any(Execution.class));
        verify(scenariosIndexerStrategy, never()).getScenariosIndexer(any(Technology.class));
    }

    @Test
    public void getExecution_returnEmpty_whenExceptionThrownWhileAccessingTheBuildInformationFile() throws IOException {
        // Given
        PlannedIndexation plannedIndexation = mock(PlannedIndexation.class);
        File executionFile = mock(File.class);
        CycleDefinition cycleDefinition = mock(CycleDefinition.class);

        File executionBuildInformationFile = mock(File.class);

        // When
        when(plannedIndexation.getExecutionFolder()).thenReturn(executionFile);
        when(executionFile.getAbsolutePath()).thenReturn("/execution/absolute/path");
        when(executionFile.listFiles()).thenReturn(new File[] {executionBuildInformationFile});
        when(executionBuildInformationFile.isFile()).thenReturn(true);
        when(executionBuildInformationFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(executionBuildInformationFile.getAbsolutePath()).thenReturn("/execution/absolute/path/to/build/information/file");
        when(plannedIndexation.getCycleDefinition()).thenReturn(cycleDefinition);
        when(objectMapper.readValue(executionBuildInformationFile, Build.class)).thenThrow(new IOException("Error while reading the execution build information file!"));

        // Then
        Optional<Execution> execution = cut.getExecution(plannedIndexation);
        assertThat(execution).isEmpty();
        verify(executionCompletionRequestRepository, never()).delete(any(ExecutionCompletionRequest.class));
        verify(qualityService, never()).computeQuality(any(Execution.class));
        verify(scenariosIndexerStrategy, never()).getScenariosIndexer(any(Technology.class));
    }

    @Test
    public void getExecution_returnEmpty_whenDoneExecutionFound() throws IOException {
        // Given
        PlannedIndexation plannedIndexation = mock(PlannedIndexation.class);
        File executionFile = mock(File.class);
        CycleDefinition cycleDefinition = mock(CycleDefinition.class);

        File executionBuildInformationFile = mock(File.class);

        Build executionBuild = mock(Build.class);

        Execution previousExecution = mock(Execution.class);

        // When
        when(plannedIndexation.getExecutionFolder()).thenReturn(executionFile);
        when(executionFile.listFiles()).thenReturn(new File[] {executionBuildInformationFile});
        when(executionBuildInformationFile.isFile()).thenReturn(true);
        when(executionBuildInformationFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(plannedIndexation.getCycleDefinition()).thenReturn(cycleDefinition);
        when(cycleDefinition.getProjectId()).thenReturn(1L);
        when(objectMapper.readValue(executionBuildInformationFile, Build.class)).thenReturn(executionBuild);
        when(executionBuild.getLink()).thenReturn("/execution/path/to/folder");
        when(executionBuild.getUrl()).thenReturn("http://build.fr/execution");
        when(executionRepository.findByProjectIdAndJobUrlOrJobLink(1L, "http://build.fr/execution", "/execution/path/to/folder")).thenReturn(Optional.of(previousExecution));
        when(previousExecution.getStatus()).thenReturn(JobStatus.DONE);
        when(executionCompletionRequestRepository.findById("http://build.fr/execution")).thenReturn(Optional.empty());

        // Then
        Optional<Execution> execution = cut.getExecution(plannedIndexation);
        assertThat(execution).isEmpty();
        verify(executionCompletionRequestRepository, never()).delete(any(ExecutionCompletionRequest.class));
        verify(qualityService, never()).computeQuality(any(Execution.class));
        verify(scenariosIndexerStrategy, never()).getScenariosIndexer(any(Technology.class));
    }

    @Test
    public void getExecution_returnEmpty_whenNoCycleDefinitionFoundAndExecutionNotDoneAndNoExecutionCompletionRequest() throws IOException {
        // Given
        PlannedIndexation plannedIndexation = mock(PlannedIndexation.class);
        File executionFile = mock(File.class);
        CycleDefinition cycleDefinition = mock(CycleDefinition.class);

        File executionBuildInformationFile = mock(File.class);

        Build executionBuild = mock(Build.class);

        Execution previousExecution = mock(Execution.class);

        File cycleDefinitionFile = mock(File.class);

        // When
        when(plannedIndexation.getExecutionFolder()).thenReturn(executionFile);
        when(executionFile.listFiles()).thenReturn(new File[] {executionBuildInformationFile, cycleDefinitionFile});
        when(executionBuildInformationFile.isFile()).thenReturn(true);
        when(executionBuildInformationFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(plannedIndexation.getCycleDefinition()).thenReturn(cycleDefinition);
        when(cycleDefinition.getProjectId()).thenReturn(1L);
        when(objectMapper.readValue(executionBuildInformationFile, Build.class)).thenReturn(executionBuild);
        when(executionBuild.getLink()).thenReturn("/execution/path/to/folder");
        when(executionBuild.getUrl()).thenReturn("http://build.fr/execution");
        when(executionRepository.findByProjectIdAndJobUrlOrJobLink(1L, "http://build.fr/execution", "/execution/path/to/folder")).thenReturn(Optional.of(previousExecution));
        when(previousExecution.getStatus()).thenReturn(JobStatus.UNAVAILABLE);
        when(previousExecution.getId()).thenReturn(1L);
        when(executionCompletionRequestRepository.findById("http://build.fr/execution")).thenReturn(Optional.empty());
        when(cycleDefinitionFile.isFile()).thenReturn(true);
        when(cycleDefinitionFile.getName()).thenReturn(CYCLE_DEFINITION_FILE_NAME);
        when(objectMapper.readValue(cycleDefinitionFile, CycleDef.class)).thenReturn(null);

        // Then
        Optional<Execution> execution = cut.getExecution(plannedIndexation);
        assertThat(execution).isEmpty();
        verify(executionCompletionRequestRepository, never()).delete(any(ExecutionCompletionRequest.class));
        verify(qualityService, never()).computeQuality(any(Execution.class));
        verify(scenariosIndexerStrategy, never()).getScenariosIndexer(any(Technology.class));
    }

    @Test
    public void getExecution_returnBlockedExecution_whenNoCycleDefinitionFoundButExecutionDoneAndNoExecutionCompletionRequest() throws IOException {
        // Given
        PlannedIndexation plannedIndexation = mock(PlannedIndexation.class);
        File executionFile = mock(File.class);
        CycleDefinition cycleDefinition = mock(CycleDefinition.class);

        File executionBuildInformationFile = mock(File.class);

        Build executionBuild = mock(Build.class);

        Execution previousExecution = mock(Execution.class);

        File cycleDefinitionFile = mock(File.class);

        // When
        when(plannedIndexation.getExecutionFolder()).thenReturn(executionFile);
        when(executionFile.listFiles()).thenReturn(new File[] {executionBuildInformationFile, cycleDefinitionFile});
        when(executionFile.getAbsolutePath()).thenReturn("/execution/path/to/cycle/definition/file");
        when(executionBuildInformationFile.isFile()).thenReturn(true);
        when(executionBuildInformationFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(plannedIndexation.getCycleDefinition()).thenReturn(cycleDefinition);
        when(cycleDefinition.getProjectId()).thenReturn(1L);
        when(cycleDefinition.getBranch()).thenReturn("develop");
        when(cycleDefinition.getName()).thenReturn("day");
        when(objectMapper.readValue(executionBuildInformationFile, Build.class)).thenReturn(executionBuild);
        when(executionBuild.getLink()).thenReturn("/execution/path/to/folder");
        when(executionBuild.getUrl()).thenReturn("http://build.fr/execution");
        when(executionBuild.getResult()).thenReturn(Result.SUCCESS);
        when(executionBuild.getRelease()).thenReturn("release-1");
        when(executionBuild.getVersion()).thenReturn("v1.0");
        when(executionBuild.isBuilding()).thenReturn(false);
        when(executionBuild.getDuration()).thenReturn(150L);
        when(executionBuild.getEstimatedDuration()).thenReturn(100L);
        when(executionBuild.getTimestamp()).thenReturn(1589200515000L);
        when(executionBuild.getVersionTimestamp()).thenReturn(1594718326000L);
        when(executionRepository.findByProjectIdAndJobUrlOrJobLink(1L, "http://build.fr/execution", "/execution/path/to/folder")).thenReturn(Optional.of(previousExecution));
        when(previousExecution.getStatus()).thenReturn(JobStatus.UNAVAILABLE);
        when(previousExecution.getId()).thenReturn(1L);
        when(executionCompletionRequestRepository.findById("http://build.fr/execution")).thenReturn(Optional.empty());
        when(cycleDefinitionFile.isFile()).thenReturn(true);
        when(cycleDefinitionFile.getName()).thenReturn(CYCLE_DEFINITION_FILE_NAME);
        when(objectMapper.readValue(cycleDefinitionFile, CycleDef.class)).thenThrow(new IOException("Couldn't read the cycle definition file!!!"));

        // Then
        Optional<Execution> execution = cut.getExecution(plannedIndexation);
        assertThat(execution).isNotEmpty();
        assertThat(execution.get().getBranch()).isEqualTo("develop");
        assertThat(execution.get().getName()).isEqualTo("day");
        assertThat(execution.get().getRelease()).isEqualTo("release-1");
        assertThat(execution.get().getVersion()).isEqualTo("v1.0");
        assertThat(execution.get().getBuildDateTime()).isEqualTo(new Date(1594718326000L));
        assertThat(execution.get().getTestDateTime()).isEqualTo(new Date(1589200515000L));
        assertThat(execution.get().getJobUrl()).isEqualTo("http://build.fr/execution");
        assertThat(execution.get().getJobLink()).isEqualTo("/execution/path/to/folder");
        assertThat(execution.get().getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(execution.get().getResult()).isEqualTo(Result.SUCCESS);
        assertThat(execution.get().getAcceptance()).isEqualTo(ExecutionAcceptance.NEW);
        assertThat(execution.get().getDiscardReason()).isNull();
        assertThat(execution.get().getCycleDefinition()).isEqualTo(cycleDefinition);
        assertThat(execution.get().getBlockingValidation()).isEqualTo(false);
        assertThat(execution.get().getQualityThresholds()).isNull();
        assertThat(execution.get().getQualityStatus()).isEqualTo(QualityStatus.INCOMPLETE);
        assertThat(execution.get().getQualitySeverities()).isNull();
        assertThat(execution.get().getDuration()).isEqualTo(150L);
        assertThat(execution.get().getEstimatedDuration()).isEqualTo(100L);
        assertThat(execution.get().getRuns()).isEmpty();
        assertThat(execution.get().getCountryDeployments()).isEmpty();
        verify(executionCompletionRequestRepository, never()).delete(any(ExecutionCompletionRequest.class));
        verify(qualityService, never()).computeQuality(any(Execution.class));
        verify(scenariosIndexerStrategy, never()).getScenariosIndexer(any(Technology.class));
    }

    @Test
    public void getExecution_returnBlockedExecution_whenNoCycleDefinitionFoundButExecutionNotDoneButExecutionCompletionRequestFound() throws IOException {
        // Given
        PlannedIndexation plannedIndexation = mock(PlannedIndexation.class);
        File executionFile = mock(File.class);
        CycleDefinition cycleDefinition = mock(CycleDefinition.class);

        File executionBuildInformationFile = mock(File.class);

        Build executionBuild = mock(Build.class);

        Execution previousExecution = mock(Execution.class);

        File cycleDefinitionFile = mock(File.class);

        ExecutionCompletionRequest executionCompletionRequest = mock(ExecutionCompletionRequest.class);

        // When
        when(plannedIndexation.getExecutionFolder()).thenReturn(executionFile);
        when(executionFile.listFiles()).thenReturn(new File[] {executionBuildInformationFile, cycleDefinitionFile});
        when(executionBuildInformationFile.isFile()).thenReturn(true);
        when(executionBuildInformationFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(plannedIndexation.getCycleDefinition()).thenReturn(cycleDefinition);
        when(cycleDefinition.getProjectId()).thenReturn(1L);
        when(cycleDefinition.getBranch()).thenReturn("develop");
        when(cycleDefinition.getName()).thenReturn("day");
        when(objectMapper.readValue(executionBuildInformationFile, Build.class)).thenReturn(executionBuild);
        when(executionBuild.getLink()).thenReturn("/execution/path/to/folder");
        when(executionBuild.getUrl()).thenReturn("http://build.fr/execution");
        when(executionBuild.getResult()).thenReturn(Result.NOT_BUILT);
        when(executionBuild.getRelease()).thenReturn("release-1");
        when(executionBuild.getVersion()).thenReturn("v1.0");
        when(executionBuild.isBuilding()).thenReturn(false);
        when(executionBuild.getDuration()).thenReturn(150L);
        when(executionBuild.getEstimatedDuration()).thenReturn(100L);
        when(executionBuild.getTimestamp()).thenReturn(1589200515000L);
        when(executionBuild.getVersionTimestamp()).thenReturn(1594718326000L);
        when(executionRepository.findByProjectIdAndJobUrlOrJobLink(1L, "http://build.fr/execution", "/execution/path/to/folder")).thenReturn(Optional.of(previousExecution));
        when(previousExecution.getStatus()).thenReturn(JobStatus.UNAVAILABLE);
        when(previousExecution.getId()).thenReturn(1L);
        when(executionCompletionRequestRepository.findById("http://build.fr/execution")).thenReturn(Optional.of(executionCompletionRequest));
        when(cycleDefinitionFile.isFile()).thenReturn(true);
        when(cycleDefinitionFile.getName()).thenReturn(CYCLE_DEFINITION_FILE_NAME);
        when(objectMapper.readValue(cycleDefinitionFile, CycleDef.class)).thenReturn(null);

        // Then
        Optional<Execution> execution = cut.getExecution(plannedIndexation);
        assertThat(execution).isNotEmpty();
        assertThat(execution.get().getBranch()).isEqualTo("develop");
        assertThat(execution.get().getName()).isEqualTo("day");
        assertThat(execution.get().getRelease()).isEqualTo("release-1");
        assertThat(execution.get().getVersion()).isEqualTo("v1.0");
        assertThat(execution.get().getBuildDateTime()).isEqualTo(new Date(1594718326000L));
        assertThat(execution.get().getTestDateTime()).isEqualTo(new Date(1589200515000L));
        assertThat(execution.get().getJobUrl()).isEqualTo("http://build.fr/execution");
        assertThat(execution.get().getJobLink()).isEqualTo("/execution/path/to/folder");
        assertThat(execution.get().getStatus()).isEqualTo(JobStatus.UNAVAILABLE);
        assertThat(execution.get().getResult()).isEqualTo(Result.NOT_BUILT);
        assertThat(execution.get().getAcceptance()).isEqualTo(ExecutionAcceptance.NEW);
        assertThat(execution.get().getDiscardReason()).isNull();
        assertThat(execution.get().getCycleDefinition()).isEqualTo(cycleDefinition);
        assertThat(execution.get().getBlockingValidation()).isEqualTo(false);
        assertThat(execution.get().getQualityThresholds()).isNull();
        assertThat(execution.get().getQualityStatus()).isEqualTo(QualityStatus.INCOMPLETE);
        assertThat(execution.get().getQualitySeverities()).isNull();
        assertThat(execution.get().getDuration()).isEqualTo(150L);
        assertThat(execution.get().getEstimatedDuration()).isEqualTo(100L);
        assertThat(execution.get().getRuns()).isEmpty();
        assertThat(execution.get().getCountryDeployments()).isEmpty();
        verify(executionCompletionRequestRepository).delete(executionCompletionRequest);
        verify(qualityService, never()).computeQuality(any(Execution.class));
        verify(scenariosIndexerStrategy, never()).getScenariosIndexer(any(Technology.class));
    }

    @Test
    public void getExecution_returnExecution_whenCycleDefinitionFoundButCountryFolderEmpty() throws IOException {
        // Given
        PlannedIndexation plannedIndexation = mock(PlannedIndexation.class);
        File executionFile = mock(File.class);
        CycleDefinition cycleDefinition = mock(CycleDefinition.class);

        File executionBuildInformationFile = mock(File.class);

        Build executionBuild = mock(Build.class);

        Execution previousExecution = mock(Execution.class);

        File cycleDefinitionFile = mock(File.class);

        CycleDef cycleDef = mock(CycleDef.class);

        Map<String, QualityThreshold> qualityThresholds = mock(Map.class);

        // When
        when(plannedIndexation.getExecutionFolder()).thenReturn(executionFile);
        when(executionFile.listFiles()).thenReturn(new File[] {executionBuildInformationFile, cycleDefinitionFile});
        when(executionBuildInformationFile.isFile()).thenReturn(true);
        when(executionBuildInformationFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(plannedIndexation.getCycleDefinition()).thenReturn(cycleDefinition);
        when(cycleDefinition.getProjectId()).thenReturn(1L);
        when(cycleDefinition.getBranch()).thenReturn("develop");
        when(cycleDefinition.getName()).thenReturn("day");
        when(objectMapper.readValue(executionBuildInformationFile, Build.class)).thenReturn(executionBuild);
        when(executionBuild.getLink()).thenReturn("/execution/path/to/folder");
        when(executionBuild.getUrl()).thenReturn("http://build.fr/execution");
        when(executionBuild.getResult()).thenReturn(Result.SUCCESS);
        when(executionBuild.getRelease()).thenReturn("release-1");
        when(executionBuild.getVersion()).thenReturn("v1.0");
        when(executionBuild.isBuilding()).thenReturn(false);
        when(executionBuild.getDuration()).thenReturn(150L);
        when(executionBuild.getEstimatedDuration()).thenReturn(100L);
        when(executionBuild.getTimestamp()).thenReturn(1589200515000L);
        when(executionBuild.getVersionTimestamp()).thenReturn(1594718326000L);
        when(executionRepository.findByProjectIdAndJobUrlOrJobLink(1L, "http://build.fr/execution", "/execution/path/to/folder")).thenReturn(Optional.of(previousExecution));
        when(previousExecution.getStatus()).thenReturn(JobStatus.UNAVAILABLE);
        when(previousExecution.getId()).thenReturn(1L);
        when(executionCompletionRequestRepository.findById("http://build.fr/execution")).thenReturn(Optional.empty());
        when(cycleDefinitionFile.isFile()).thenReturn(true);
        when(cycleDefinitionFile.getName()).thenReturn(CYCLE_DEFINITION_FILE_NAME);
        when(objectMapper.readValue(cycleDefinitionFile, CycleDef.class)).thenReturn(cycleDef);
        when(cycleDef.isBlockingValidation()).thenReturn(true);
        when(cycleDef.getQualityThresholds()).thenReturn(qualityThresholds);
        when(objectMapper.writeValueAsString(qualityThresholds)).thenReturn("final-quality-threshold");

        // Then
        Optional<Execution> execution = cut.getExecution(plannedIndexation);
        assertThat(execution).isNotEmpty();
        assertThat(execution.get().getBranch()).isEqualTo("develop");
        assertThat(execution.get().getName()).isEqualTo("day");
        assertThat(execution.get().getRelease()).isEqualTo("release-1");
        assertThat(execution.get().getVersion()).isEqualTo("v1.0");
        assertThat(execution.get().getBuildDateTime()).isEqualTo(new Date(1594718326000L));
        assertThat(execution.get().getTestDateTime()).isEqualTo(new Date(1589200515000L));
        assertThat(execution.get().getJobUrl()).isEqualTo("http://build.fr/execution");
        assertThat(execution.get().getJobLink()).isEqualTo("/execution/path/to/folder");
        assertThat(execution.get().getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(execution.get().getResult()).isEqualTo(Result.SUCCESS);
        assertThat(execution.get().getAcceptance()).isEqualTo(ExecutionAcceptance.NEW);
        assertThat(execution.get().getDiscardReason()).isNull();
        assertThat(execution.get().getCycleDefinition()).isEqualTo(cycleDefinition);
        assertThat(execution.get().getBlockingValidation()).isEqualTo(true);
        assertThat(execution.get().getQualityThresholds()).isEqualTo("final-quality-threshold");
        assertThat(execution.get().getQualityStatus()).isEqualTo(QualityStatus.INCOMPLETE);
        assertThat(execution.get().getQualitySeverities()).isNull();
        assertThat(execution.get().getDuration()).isEqualTo(150L);
        assertThat(execution.get().getEstimatedDuration()).isEqualTo(100L);
        assertThat(execution.get().getRuns()).isEmpty();
        assertThat(execution.get().getCountryDeployments()).isEmpty();
        verify(executionCompletionRequestRepository, never()).delete(any(ExecutionCompletionRequest.class));
        verify(qualityService).computeQuality(execution.get());
        verify(scenariosIndexerStrategy, never()).getScenariosIndexer(any(Technology.class));
    }

    @Test
    public void getExecution_returnExecution_whenCycleDefinitionFoundAndCountryFolderAndTypeFolderAreNotEmptyAndCorrect() throws IOException {
        // Given
        PlannedIndexation plannedIndexation = mock(PlannedIndexation.class);
        File executionFile = mock(File.class);
        CycleDefinition cycleDefinition = mock(CycleDefinition.class);

        File executionBuildInformationFile = mock(File.class);

        Build executionBuild = mock(Build.class);

        Execution previousExecution = mock(Execution.class);

        File cycleDefinitionFile = mock(File.class);

        CycleDef cycleDef = mock(CycleDef.class);

        Map<String, QualityThreshold> qualityThresholds = mock(Map.class);

        Country frCountry = mock(Country.class);
        Country esCountry = mock(Country.class);
        Country deCountry = mock(Country.class);

        Type apiType = mock(Type.class);
        Source apiSource = mock(Source.class);
        Type desktopType = mock(Type.class);
        Source desktopSource = mock(Source.class);
        Type mobileType = mock(Type.class);
        Source mobileSource = mock(Source.class);
        Type anotherType = mock(Type.class);

        File esFolder = mock(File.class);
        File frFolder = mock(File.class);

        File apiTypeFolder = mock(File.class);
        File desktopTypeFolder = mock(File.class);
        File mobileTypeFolder = mock(File.class);

        PlatformRule platformRule11 = mock(PlatformRule.class);
        PlatformRule platformRule12 = mock(PlatformRule.class);
        PlatformRule platformRule21 = mock(PlatformRule.class);

        Map<String, List<PlatformRule>> platformRules = new HashMap<String, List<PlatformRule>>(){{
            put("integration-1", Arrays.asList(platformRule11, platformRule12));
            put("integration-2", Arrays.asList(platformRule21));
        }};

        File esBuildFile = mock(File.class);
        File frBuildFile = mock(File.class);

        File apiBuildFile = mock(File.class);
        File desktopBuildFile = mock(File.class);
        File mobileBuildFile = mock(File.class);

        Build esBuild = mock(Build.class);
        Build frBuild = mock(Build.class);

        Build apiBuild = mock(Build.class);
        Build desktopBuild = mock(Build.class);
        Build mobileBuild = mock(Build.class);

        // When
        when(plannedIndexation.getExecutionFolder()).thenReturn(executionFile);
        when(executionFile.listFiles()).thenReturn(
                new File[] {
                        executionBuildInformationFile,
                        cycleDefinitionFile,
                        esFolder,
                        frFolder
                });
        when(executionBuildInformationFile.isFile()).thenReturn(true);
        when(executionBuildInformationFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(plannedIndexation.getCycleDefinition()).thenReturn(cycleDefinition);
        when(cycleDefinition.getProjectId()).thenReturn(1L);
        when(cycleDefinition.getBranch()).thenReturn("develop");
        when(cycleDefinition.getName()).thenReturn("day");

        when(objectMapper.readValue(executionBuildInformationFile, Build.class)).thenReturn(executionBuild);
        when(executionBuild.getLink()).thenReturn("/execution/path/to/folder");
        when(executionBuild.getUrl()).thenReturn("http://build.fr/execution");
        when(executionBuild.getResult()).thenReturn(Result.SUCCESS);
        when(executionBuild.getRelease()).thenReturn("release-1");
        when(executionBuild.getVersion()).thenReturn("v1.0");
        when(executionBuild.isBuilding()).thenReturn(false);
        when(executionBuild.getDuration()).thenReturn(150L);
        when(executionBuild.getEstimatedDuration()).thenReturn(100L);
        when(executionBuild.getTimestamp()).thenReturn(1589200515000L);
        when(executionBuild.getVersionTimestamp()).thenReturn(1594718326000L);

        when(executionRepository.findByProjectIdAndJobUrlOrJobLink(1L, "http://build.fr/execution", "/execution/path/to/folder")).thenReturn(Optional.of(previousExecution));
        when(previousExecution.getStatus()).thenReturn(JobStatus.UNAVAILABLE);
        when(previousExecution.getId()).thenReturn(1L);
        when(executionCompletionRequestRepository.findById("http://build.fr/execution")).thenReturn(Optional.empty());
        when(cycleDefinitionFile.isFile()).thenReturn(true);
        when(cycleDefinitionFile.getName()).thenReturn(CYCLE_DEFINITION_FILE_NAME);
        when(objectMapper.readValue(cycleDefinitionFile, CycleDef.class)).thenReturn(cycleDef);
        when(cycleDef.isBlockingValidation()).thenReturn(true);
        when(cycleDef.getPlatformsRules()).thenReturn(platformRules);
        when(cycleDef.getQualityThresholds()).thenReturn(qualityThresholds);
        when(objectMapper.writeValueAsString(qualityThresholds)).thenReturn("final-quality-threshold");

        when(platformRule11.isEnabled()).thenReturn(true);
        when(platformRule11.getCountry()).thenReturn("ES");
        when(platformRule11.getTestTypes()).thenReturn("API");
        when(platformRule11.getCountryTags()).thenReturn("all-countries");
        when(platformRule11.getSeverityTags()).thenReturn("all-severities");
        when(platformRule11.isBlockingValidation()).thenReturn(true);
        when(platformRule12.isEnabled()).thenReturn(false);
        when(platformRule21.isEnabled()).thenReturn(true);
        when(platformRule21.getCountry()).thenReturn("fr");
        when(platformRule21.getTestTypes()).thenReturn("mobile,desktop");
        when(platformRule21.getCountryTags()).thenReturn("fr");
        when(platformRule21.getSeverityTags()).thenReturn("sanity");
        when(platformRule21.isBlockingValidation()).thenReturn(false);

        when(frCountry.getCode()).thenReturn("fr");
        when(esCountry.getCode()).thenReturn("es");

        when(apiType.getCode()).thenReturn("api");
        when(apiType.getSource()).thenReturn(apiSource);
        when(apiSource.getTechnology()).thenReturn(Technology.POSTMAN);
        when(desktopType.getCode()).thenReturn("desktop");
        when(desktopType.getSource()).thenReturn(desktopSource);
        when(desktopSource.getTechnology()).thenReturn(Technology.CUCUMBER);
        when(mobileType.getCode()).thenReturn("mobile");
        when(mobileType.getSource()).thenReturn(mobileSource);
        when(mobileSource.getTechnology()).thenReturn(Technology.CUCUMBER);

        when(countryRepository.findAllByProjectIdOrderByCode(1L)).thenReturn(Arrays.asList(frCountry, esCountry, deCountry));
        when(typeRepository.findAllByProjectIdOrderByCode(1L)).thenReturn(Arrays.asList(apiType, desktopType, mobileType, anotherType));

        when(esBuildFile.isFile()).thenReturn(true);
        when(esBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(esBuildFile, Build.class)).thenReturn(esBuild);
        when(esBuild.getLink()).thenReturn("/execution/path/to/folder/es");
        when(esBuild.getUrl()).thenReturn("http://build.fr/execution/es");
        when(esBuild.getResult()).thenReturn(Result.SUCCESS);
        when(esBuild.isBuilding()).thenReturn(true);
        when(esBuild.getDuration()).thenReturn(200L);
        when(esBuild.getEstimatedDuration()).thenReturn(300L);
        when(esBuild.getTimestamp()).thenReturn(1590200515000L);

        when(frBuildFile.isFile()).thenReturn(true);
        when(frBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(frBuildFile, Build.class)).thenReturn(frBuild);
        when(frBuild.getLink()).thenReturn("/execution/path/to/folder/fr");
        when(frBuild.getUrl()).thenReturn("http://build.fr/execution/fr");
        when(frBuild.getResult()).thenReturn(Result.UNSTABLE);
        when(frBuild.isBuilding()).thenReturn(false);
        when(frBuild.getDuration()).thenReturn(500L);
        when(frBuild.getEstimatedDuration()).thenReturn(99L);
        when(frBuild.getTimestamp()).thenReturn(1589211515000L);

        when(esFolder.isDirectory()).thenReturn(true);
        when(esFolder.getName()).thenReturn("es");
        when(esFolder.listFiles()).thenReturn(new File[]{apiTypeFolder, esBuildFile});
        when(frFolder.isDirectory()).thenReturn(true);
        when(frFolder.getName()).thenReturn("fr");
        when(frFolder.listFiles()).thenReturn(new File[]{desktopTypeFolder, mobileTypeFolder, frBuildFile});

        when(apiBuildFile.isFile()).thenReturn(true);
        when(apiBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(apiBuildFile, Build.class)).thenReturn(apiBuild);
        when(apiBuild.getLink()).thenReturn("/execution/path/to/folder/api");
        when(apiBuild.getUrl()).thenReturn("http://build.fr/execution/api");
        when(apiBuild.isBuilding()).thenReturn(true);
        when(apiBuild.getDuration()).thenReturn(650L);
        when(apiBuild.getEstimatedDuration()).thenReturn(185L);
        when(apiBuild.getTimestamp()).thenReturn(1666200515000L);
        when(apiBuild.getComment()).thenReturn("comment for api build");

        when(desktopBuildFile.isFile()).thenReturn(true);
        when(desktopBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(desktopBuildFile, Build.class)).thenReturn(desktopBuild);
        when(desktopBuild.getLink()).thenReturn("/execution/path/to/folder/desktop");
        when(desktopBuild.getUrl()).thenReturn("http://build.fr/execution/desktop");
        when(desktopBuild.isBuilding()).thenReturn(false);
        when(desktopBuild.getDuration()).thenReturn(350L);
        when(desktopBuild.getEstimatedDuration()).thenReturn(745L);
        when(desktopBuild.getTimestamp()).thenReturn(1589230515100L);
        when(desktopBuild.getComment()).thenReturn("comment for desktop build");

        when(mobileBuildFile.isFile()).thenReturn(true);
        when(mobileBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(mobileBuildFile, Build.class)).thenReturn(mobileBuild);
        when(mobileBuild.getLink()).thenReturn("/execution/path/to/folder/mobile");
        when(mobileBuild.getUrl()).thenReturn("http://build.fr/execution/mobile");
        when(mobileBuild.isBuilding()).thenReturn(true);
        when(mobileBuild.getDuration()).thenReturn(75L);
        when(mobileBuild.getEstimatedDuration()).thenReturn(3050L);
        when(mobileBuild.getTimestamp()).thenReturn(1512345515000L);
        when(mobileBuild.getComment()).thenReturn("comment for mobile build");

        when(apiTypeFolder.isDirectory()).thenReturn(true);
        when(apiTypeFolder.getName()).thenReturn("api");
        when(apiTypeFolder.listFiles()).thenReturn(new File[]{apiBuildFile});
        when(desktopTypeFolder.isDirectory()).thenReturn(true);
        when(desktopTypeFolder.getName()).thenReturn("desktop");
        when(desktopTypeFolder.listFiles()).thenReturn(new File[]{desktopBuildFile});
        when(mobileTypeFolder.isDirectory()).thenReturn(true);
        when(mobileTypeFolder.getName()).thenReturn("mobile");
        when(mobileTypeFolder.listFiles()).thenReturn(new File[]{mobileBuildFile});

        // Then
        Optional<Execution> execution = cut.getExecution(plannedIndexation);
        assertThat(execution).isNotEmpty();
        assertThat(execution.get().getBranch()).isEqualTo("develop");
        assertThat(execution.get().getName()).isEqualTo("day");
        assertThat(execution.get().getRelease()).isEqualTo("release-1");
        assertThat(execution.get().getVersion()).isEqualTo("v1.0");
        assertThat(execution.get().getBuildDateTime()).isEqualTo(new Date(1594718326000L));
        assertThat(execution.get().getTestDateTime()).isEqualTo(new Date(1589200515000L));
        assertThat(execution.get().getJobUrl()).isEqualTo("http://build.fr/execution");
        assertThat(execution.get().getJobLink()).isEqualTo("/execution/path/to/folder");
        assertThat(execution.get().getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(execution.get().getResult()).isEqualTo(Result.SUCCESS);
        assertThat(execution.get().getAcceptance()).isEqualTo(ExecutionAcceptance.NEW);
        assertThat(execution.get().getDiscardReason()).isNull();
        assertThat(execution.get().getCycleDefinition()).isEqualTo(cycleDefinition);
        assertThat(execution.get().getBlockingValidation()).isEqualTo(true);
        assertThat(execution.get().getQualityThresholds()).isEqualTo("final-quality-threshold");
        assertThat(execution.get().getQualityStatus()).isEqualTo(QualityStatus.INCOMPLETE);
        assertThat(execution.get().getQualitySeverities()).isNull();
        assertThat(execution.get().getDuration()).isEqualTo(150L);
        assertThat(execution.get().getEstimatedDuration()).isEqualTo(100L);
        assertThat(execution.get().getRuns()).hasSize(3)
                .extracting(
                        "country",
                        "type",
                        "comment",
                        "platform",
                        "jobUrl",
                        "jobLink",
                        "status",
                        "countryTags",
                        "startDateTime",
                        "estimatedDuration",
                        "duration",
                        "severityTags",
                        "includeInThresholds"
                )
                .containsOnly(
                        tuple(
                                esCountry,
                                apiType,
                                "comment for api build",
                                "integration-1",
                                "http://build.fr/execution/api",
                                "/execution/path/to/folder/api",
                                JobStatus.DONE,
                                "all-countries",
                                new Date(1666200515000L),
                                185L,
                                650L,
                                "all-severities",
                                true
                        ),
                        tuple(
                                frCountry,
                                desktopType,
                                "comment for desktop build",
                                "integration-2",
                                "http://build.fr/execution/desktop",
                                "/execution/path/to/folder/desktop",
                                JobStatus.DONE,
                                "fr",
                                new Date(1589230515100L),
                                745L,
                                350L,
                                "sanity",
                                false
                        ),
                        tuple(
                                frCountry,
                                mobileType,
                                "comment for mobile build",
                                "integration-2",
                                "http://build.fr/execution/mobile",
                                "/execution/path/to/folder/mobile",
                                JobStatus.DONE,
                                "fr",
                                new Date(1512345515000L),
                                3050L,
                                75L,
                                "sanity",
                                false
                        )
                );
        assertThat(execution.get().getCountryDeployments())
                .hasSize(2)
                .extracting(
                        "country",
                        "platform",
                        "jobUrl",
                        "jobLink",
                        "status",
                        "result",
                        "startDateTime",
                        "estimatedDuration",
                        "duration"
                )
                .containsOnly(
                        tuple(
                                esCountry,
                                "integration-1",
                                "http://build.fr/execution/es",
                                "/execution/path/to/folder/es",
                                JobStatus.DONE,
                                Result.SUCCESS,
                                new Date(1590200515000L),
                                300L,
                                200L
                        ),
                        tuple(
                                frCountry,
                                "integration-2",
                                "http://build.fr/execution/fr",
                                "/execution/path/to/folder/fr",
                                JobStatus.DONE,
                                Result.UNSTABLE,
                                new Date(1589211515000L),
                                99L,
                                500L

                        )
                );
        verify(executionCompletionRequestRepository, never()).delete(any(ExecutionCompletionRequest.class));
        verify(qualityService).computeQuality(execution.get());
        verify(scenariosIndexerStrategy, times(2)).getScenariosIndexer(Technology.CUCUMBER);
        verify(scenariosIndexerStrategy).getScenariosIndexer(Technology.POSTMAN);
    }

    @Test
    public void getExecution_returnExecution_whenCycleDefinitionFoundAndCountryIsNotInDBAndTypeFound() throws IOException {
        // Given
        PlannedIndexation plannedIndexation = mock(PlannedIndexation.class);
        File executionFile = mock(File.class);
        CycleDefinition cycleDefinition = mock(CycleDefinition.class);

        File executionBuildInformationFile = mock(File.class);

        Build executionBuild = mock(Build.class);

        Execution previousExecution = mock(Execution.class);

        File cycleDefinitionFile = mock(File.class);

        CycleDef cycleDef = mock(CycleDef.class);

        Map<String, QualityThreshold> qualityThresholds = mock(Map.class);

        Country esCountry = mock(Country.class);
        Country deCountry = mock(Country.class);

        Type apiType = mock(Type.class);
        Source apiSource = mock(Source.class);
        Type desktopType = mock(Type.class);
        Type mobileType = mock(Type.class);
        Type anotherType = mock(Type.class);

        File esFolder = mock(File.class);
        File frFolder = mock(File.class);

        File apiTypeFolder = mock(File.class);

        PlatformRule platformRule11 = mock(PlatformRule.class);
        PlatformRule platformRule12 = mock(PlatformRule.class);
        PlatformRule platformRule21 = mock(PlatformRule.class);

        Map<String, List<PlatformRule>> platformRules = new HashMap<String, List<PlatformRule>>(){{
            put("integration-1", Arrays.asList(platformRule11, platformRule12));
            put("integration-2", Arrays.asList(platformRule21));
        }};

        File esBuildFile = mock(File.class);

        File apiBuildFile = mock(File.class);

        Build esBuild = mock(Build.class);

        Build apiBuild = mock(Build.class);

        // When
        when(plannedIndexation.getExecutionFolder()).thenReturn(executionFile);
        when(executionFile.listFiles()).thenReturn(
                new File[] {
                        executionBuildInformationFile,
                        cycleDefinitionFile,
                        esFolder,
                        frFolder
                });
        when(executionBuildInformationFile.isFile()).thenReturn(true);
        when(executionBuildInformationFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(plannedIndexation.getCycleDefinition()).thenReturn(cycleDefinition);
        when(cycleDefinition.getProjectId()).thenReturn(1L);
        when(cycleDefinition.getBranch()).thenReturn("develop");
        when(cycleDefinition.getName()).thenReturn("day");

        when(objectMapper.readValue(executionBuildInformationFile, Build.class)).thenReturn(executionBuild);
        when(executionBuild.getLink()).thenReturn("/execution/path/to/folder");
        when(executionBuild.getUrl()).thenReturn("http://build.fr/execution");
        when(executionBuild.getResult()).thenReturn(Result.SUCCESS);
        when(executionBuild.getRelease()).thenReturn("release-1");
        when(executionBuild.getVersion()).thenReturn("v1.0");
        when(executionBuild.isBuilding()).thenReturn(false);
        when(executionBuild.getDuration()).thenReturn(150L);
        when(executionBuild.getEstimatedDuration()).thenReturn(100L);
        when(executionBuild.getTimestamp()).thenReturn(1589200515000L);
        when(executionBuild.getVersionTimestamp()).thenReturn(1594718326000L);

        when(executionRepository.findByProjectIdAndJobUrlOrJobLink(1L, "http://build.fr/execution", "/execution/path/to/folder")).thenReturn(Optional.of(previousExecution));
        when(previousExecution.getStatus()).thenReturn(JobStatus.UNAVAILABLE);
        when(previousExecution.getId()).thenReturn(1L);
        when(executionCompletionRequestRepository.findById("http://build.fr/execution")).thenReturn(Optional.empty());
        when(cycleDefinitionFile.isFile()).thenReturn(true);
        when(cycleDefinitionFile.getName()).thenReturn(CYCLE_DEFINITION_FILE_NAME);
        when(objectMapper.readValue(cycleDefinitionFile, CycleDef.class)).thenReturn(cycleDef);
        when(cycleDef.isBlockingValidation()).thenReturn(true);
        when(cycleDef.getPlatformsRules()).thenReturn(platformRules);
        when(cycleDef.getQualityThresholds()).thenReturn(qualityThresholds);
        when(objectMapper.writeValueAsString(qualityThresholds)).thenReturn("final-quality-threshold");

        when(platformRule11.isEnabled()).thenReturn(true);
        when(platformRule11.getCountry()).thenReturn("es");
        when(platformRule11.getTestTypes()).thenReturn("api");
        when(platformRule11.getCountryTags()).thenReturn("all-countries");
        when(platformRule11.getSeverityTags()).thenReturn("all-severities");
        when(platformRule11.isBlockingValidation()).thenReturn(true);
        when(platformRule12.isEnabled()).thenReturn(false);
        when(platformRule21.isEnabled()).thenReturn(true);
        when(platformRule21.getCountry()).thenReturn("fr");

        when(esCountry.getCode()).thenReturn("es");
        when(deCountry.getCode()).thenReturn("de");

        when(apiType.getCode()).thenReturn("api");
        when(apiType.getSource()).thenReturn(apiSource);
        when(apiSource.getTechnology()).thenReturn(Technology.POSTMAN);

        when(countryRepository.findAllByProjectIdOrderByCode(1L)).thenReturn(Arrays.asList(deCountry, esCountry));
        when(typeRepository.findAllByProjectIdOrderByCode(1L)).thenReturn(Arrays.asList(apiType, desktopType, mobileType, anotherType));

        when(esBuildFile.isFile()).thenReturn(true);
        when(esBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(esBuildFile, Build.class)).thenReturn(esBuild);
        when(esBuild.getLink()).thenReturn("/execution/path/to/folder/es");
        when(esBuild.getUrl()).thenReturn("http://build.fr/execution/es");
        when(esBuild.getResult()).thenReturn(Result.SUCCESS);
        when(esBuild.isBuilding()).thenReturn(true);
        when(esBuild.getDuration()).thenReturn(200L);
        when(esBuild.getEstimatedDuration()).thenReturn(300L);
        when(esBuild.getTimestamp()).thenReturn(1590200515000L);

        when(esFolder.isDirectory()).thenReturn(true);
        when(esFolder.getName()).thenReturn("es");
        when(esFolder.listFiles()).thenReturn(new File[]{apiTypeFolder, esBuildFile});
        when(frFolder.isDirectory()).thenReturn(true);

        when(apiBuildFile.isFile()).thenReturn(true);
        when(apiBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(apiBuildFile, Build.class)).thenReturn(apiBuild);
        when(apiBuild.getLink()).thenReturn("/execution/path/to/folder/api");
        when(apiBuild.getUrl()).thenReturn("http://build.fr/execution/api");
        when(apiBuild.isBuilding()).thenReturn(true);
        when(apiBuild.getDuration()).thenReturn(650L);
        when(apiBuild.getEstimatedDuration()).thenReturn(185L);
        when(apiBuild.getTimestamp()).thenReturn(1666200515000L);
        when(apiBuild.getComment()).thenReturn("comment for api build");

        when(apiTypeFolder.isDirectory()).thenReturn(true);
        when(apiTypeFolder.getName()).thenReturn("api");
        when(apiTypeFolder.listFiles()).thenReturn(new File[]{apiBuildFile});

        // Then
        Optional<Execution> execution = cut.getExecution(plannedIndexation);
        assertThat(execution).isNotEmpty();
        assertThat(execution.get().getBranch()).isEqualTo("develop");
        assertThat(execution.get().getName()).isEqualTo("day");
        assertThat(execution.get().getRelease()).isEqualTo("release-1");
        assertThat(execution.get().getVersion()).isEqualTo("v1.0");
        assertThat(execution.get().getBuildDateTime()).isEqualTo(new Date(1594718326000L));
        assertThat(execution.get().getTestDateTime()).isEqualTo(new Date(1589200515000L));
        assertThat(execution.get().getJobUrl()).isEqualTo("http://build.fr/execution");
        assertThat(execution.get().getJobLink()).isEqualTo("/execution/path/to/folder");
        assertThat(execution.get().getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(execution.get().getResult()).isEqualTo(Result.SUCCESS);
        assertThat(execution.get().getAcceptance()).isEqualTo(ExecutionAcceptance.NEW);
        assertThat(execution.get().getDiscardReason()).isNull();
        assertThat(execution.get().getCycleDefinition()).isEqualTo(cycleDefinition);
        assertThat(execution.get().getBlockingValidation()).isEqualTo(true);
        assertThat(execution.get().getQualityThresholds()).isEqualTo("final-quality-threshold");
        assertThat(execution.get().getQualityStatus()).isEqualTo(QualityStatus.INCOMPLETE);
        assertThat(execution.get().getQualitySeverities()).isNull();
        assertThat(execution.get().getDuration()).isEqualTo(150L);
        assertThat(execution.get().getEstimatedDuration()).isEqualTo(100L);
        assertThat(execution.get().getRuns()).hasSize(1)
                .extracting(
                        "country",
                        "type",
                        "comment",
                        "platform",
                        "jobUrl",
                        "jobLink",
                        "status",
                        "countryTags",
                        "startDateTime",
                        "estimatedDuration",
                        "duration",
                        "severityTags",
                        "includeInThresholds"
                )
                .containsOnly(
                        tuple(
                                esCountry,
                                apiType,
                                "comment for api build",
                                "integration-1",
                                "http://build.fr/execution/api",
                                "/execution/path/to/folder/api",
                                JobStatus.DONE,
                                "all-countries",
                                new Date(1666200515000L),
                                185L,
                                650L,
                                "all-severities",
                                true
                        )
                );
        assertThat(execution.get().getCountryDeployments())
                .hasSize(1)
                .extracting(
                        "country",
                        "platform",
                        "jobUrl",
                        "jobLink",
                        "status",
                        "result",
                        "startDateTime",
                        "estimatedDuration",
                        "duration"
                )
                .containsOnly(
                        tuple(
                                esCountry,
                                "integration-1",
                                "http://build.fr/execution/es",
                                "/execution/path/to/folder/es",
                                JobStatus.DONE,
                                Result.SUCCESS,
                                new Date(1590200515000L),
                                300L,
                                200L
                        )
                );
        verify(executionCompletionRequestRepository, never()).delete(any(ExecutionCompletionRequest.class));
        verify(qualityService).computeQuality(execution.get());
        verify(scenariosIndexerStrategy, never()).getScenariosIndexer(Technology.CUCUMBER);
        verify(scenariosIndexerStrategy).getScenariosIndexer(Technology.POSTMAN);
    }

    @Test
    public void getExecution_returnExecution_whenCycleDefinitionFoundAndCountryFolderNotFoundButTypeFolderCorrect() throws IOException {
        // Given
        PlannedIndexation plannedIndexation = mock(PlannedIndexation.class);
        File executionFile = mock(File.class);
        CycleDefinition cycleDefinition = mock(CycleDefinition.class);

        File executionBuildInformationFile = mock(File.class);

        Build executionBuild = mock(Build.class);

        Execution previousExecution = mock(Execution.class);

        File cycleDefinitionFile = mock(File.class);

        CycleDef cycleDef = mock(CycleDef.class);

        Map<String, QualityThreshold> qualityThresholds = mock(Map.class);

        Country frCountry = mock(Country.class);
        Country esCountry = mock(Country.class);
        Country deCountry = mock(Country.class);

        Type apiType = mock(Type.class);
        Source apiSource = mock(Source.class);
        Type desktopType = mock(Type.class);
        Type mobileType = mock(Type.class);
        Type anotherType = mock(Type.class);

        File esFolder = mock(File.class);

        File apiTypeFolder = mock(File.class);

        PlatformRule platformRule11 = mock(PlatformRule.class);
        PlatformRule platformRule12 = mock(PlatformRule.class);
        PlatformRule platformRule21 = mock(PlatformRule.class);

        Map<String, List<PlatformRule>> platformRules = new HashMap<String, List<PlatformRule>>(){{
            put("integration-1", Arrays.asList(platformRule11, platformRule12));
            put("integration-2", Arrays.asList(platformRule21));
        }};

        File esBuildFile = mock(File.class);

        File apiBuildFile = mock(File.class);

        Build esBuild = mock(Build.class);

        Build apiBuild = mock(Build.class);

        // When
        when(plannedIndexation.getExecutionFolder()).thenReturn(executionFile);
        when(executionFile.listFiles()).thenReturn(
                new File[] {
                        executionBuildInformationFile,
                        cycleDefinitionFile,
                        esFolder
                });
        when(executionBuildInformationFile.isFile()).thenReturn(true);
        when(executionBuildInformationFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(plannedIndexation.getCycleDefinition()).thenReturn(cycleDefinition);
        when(cycleDefinition.getProjectId()).thenReturn(1L);
        when(cycleDefinition.getBranch()).thenReturn("develop");
        when(cycleDefinition.getName()).thenReturn("day");

        when(objectMapper.readValue(executionBuildInformationFile, Build.class)).thenReturn(executionBuild);
        when(executionBuild.getLink()).thenReturn("/execution/path/to/folder");
        when(executionBuild.getUrl()).thenReturn("http://build.fr/execution");
        when(executionBuild.getResult()).thenReturn(Result.SUCCESS);
        when(executionBuild.getRelease()).thenReturn("release-1");
        when(executionBuild.getVersion()).thenReturn("v1.0");
        when(executionBuild.isBuilding()).thenReturn(false);
        when(executionBuild.getDuration()).thenReturn(150L);
        when(executionBuild.getEstimatedDuration()).thenReturn(100L);
        when(executionBuild.getTimestamp()).thenReturn(1589200515000L);
        when(executionBuild.getVersionTimestamp()).thenReturn(1594718326000L);

        when(executionRepository.findByProjectIdAndJobUrlOrJobLink(1L, "http://build.fr/execution", "/execution/path/to/folder")).thenReturn(Optional.of(previousExecution));
        when(previousExecution.getStatus()).thenReturn(JobStatus.UNAVAILABLE);
        when(previousExecution.getId()).thenReturn(1L);
        when(executionCompletionRequestRepository.findById("http://build.fr/execution")).thenReturn(Optional.empty());
        when(cycleDefinitionFile.isFile()).thenReturn(true);
        when(cycleDefinitionFile.getName()).thenReturn(CYCLE_DEFINITION_FILE_NAME);
        when(objectMapper.readValue(cycleDefinitionFile, CycleDef.class)).thenReturn(cycleDef);
        when(cycleDef.isBlockingValidation()).thenReturn(true);
        when(cycleDef.getPlatformsRules()).thenReturn(platformRules);
        when(cycleDef.getQualityThresholds()).thenReturn(qualityThresholds);
        when(objectMapper.writeValueAsString(qualityThresholds)).thenReturn("final-quality-threshold");

        when(platformRule11.isEnabled()).thenReturn(true);
        when(platformRule11.getCountry()).thenReturn("es");
        when(platformRule11.getTestTypes()).thenReturn("api");
        when(platformRule11.getCountryTags()).thenReturn("all-countries");
        when(platformRule11.getSeverityTags()).thenReturn("all-severities");
        when(platformRule11.isBlockingValidation()).thenReturn(true);
        when(platformRule12.isEnabled()).thenReturn(false);
        when(platformRule21.isEnabled()).thenReturn(true);
        when(platformRule21.getCountry()).thenReturn("fr");

        when(frCountry.getCode()).thenReturn("fr");
        when(esCountry.getCode()).thenReturn("es");

        when(apiType.getCode()).thenReturn("api");
        when(apiType.getSource()).thenReturn(apiSource);
        when(apiSource.getTechnology()).thenReturn(Technology.POSTMAN);

        when(countryRepository.findAllByProjectIdOrderByCode(1L)).thenReturn(Arrays.asList(frCountry, esCountry, deCountry));
        when(typeRepository.findAllByProjectIdOrderByCode(1L)).thenReturn(Arrays.asList(apiType, desktopType, mobileType, anotherType));

        when(esBuildFile.isFile()).thenReturn(true);
        when(esBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(esBuildFile, Build.class)).thenReturn(esBuild);
        when(esBuild.getLink()).thenReturn("/execution/path/to/folder/es");
        when(esBuild.getUrl()).thenReturn("http://build.fr/execution/es");
        when(esBuild.getResult()).thenReturn(Result.SUCCESS);
        when(esBuild.isBuilding()).thenReturn(true);
        when(esBuild.getDuration()).thenReturn(200L);
        when(esBuild.getEstimatedDuration()).thenReturn(300L);
        when(esBuild.getTimestamp()).thenReturn(1590200515000L);

        when(esFolder.isDirectory()).thenReturn(true);
        when(esFolder.getName()).thenReturn("es");
        when(esFolder.listFiles()).thenReturn(new File[]{apiTypeFolder, esBuildFile});

        when(apiBuildFile.isFile()).thenReturn(true);
        when(apiBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(apiBuildFile, Build.class)).thenReturn(apiBuild);
        when(apiBuild.getLink()).thenReturn("/execution/path/to/folder/api");
        when(apiBuild.getUrl()).thenReturn("http://build.fr/execution/api");
        when(apiBuild.isBuilding()).thenReturn(true);
        when(apiBuild.getDuration()).thenReturn(650L);
        when(apiBuild.getEstimatedDuration()).thenReturn(185L);
        when(apiBuild.getTimestamp()).thenReturn(1666200515000L);
        when(apiBuild.getComment()).thenReturn("comment for api build");

        when(apiTypeFolder.isDirectory()).thenReturn(true);
        when(apiTypeFolder.getName()).thenReturn("api");
        when(apiTypeFolder.listFiles()).thenReturn(new File[]{apiBuildFile});

        // Then
        Optional<Execution> execution = cut.getExecution(plannedIndexation);
        assertThat(execution).isNotEmpty();
        assertThat(execution.get().getBranch()).isEqualTo("develop");
        assertThat(execution.get().getName()).isEqualTo("day");
        assertThat(execution.get().getRelease()).isEqualTo("release-1");
        assertThat(execution.get().getVersion()).isEqualTo("v1.0");
        assertThat(execution.get().getBuildDateTime()).isEqualTo(new Date(1594718326000L));
        assertThat(execution.get().getTestDateTime()).isEqualTo(new Date(1589200515000L));
        assertThat(execution.get().getJobUrl()).isEqualTo("http://build.fr/execution");
        assertThat(execution.get().getJobLink()).isEqualTo("/execution/path/to/folder");
        assertThat(execution.get().getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(execution.get().getResult()).isEqualTo(Result.SUCCESS);
        assertThat(execution.get().getAcceptance()).isEqualTo(ExecutionAcceptance.NEW);
        assertThat(execution.get().getDiscardReason()).isNull();
        assertThat(execution.get().getCycleDefinition()).isEqualTo(cycleDefinition);
        assertThat(execution.get().getBlockingValidation()).isEqualTo(true);
        assertThat(execution.get().getQualityThresholds()).isEqualTo("final-quality-threshold");
        assertThat(execution.get().getQualityStatus()).isEqualTo(QualityStatus.INCOMPLETE);
        assertThat(execution.get().getQualitySeverities()).isNull();
        assertThat(execution.get().getDuration()).isEqualTo(150L);
        assertThat(execution.get().getEstimatedDuration()).isEqualTo(100L);
        assertThat(execution.get().getRuns()).hasSize(1)
                .extracting(
                        "country",
                        "type",
                        "comment",
                        "platform",
                        "jobUrl",
                        "jobLink",
                        "status",
                        "countryTags",
                        "startDateTime",
                        "estimatedDuration",
                        "duration",
                        "severityTags",
                        "includeInThresholds"
                )
                .containsOnly(
                        tuple(
                                esCountry,
                                apiType,
                                "comment for api build",
                                "integration-1",
                                "http://build.fr/execution/api",
                                "/execution/path/to/folder/api",
                                JobStatus.DONE,
                                "all-countries",
                                new Date(1666200515000L),
                                185L,
                                650L,
                                "all-severities",
                                true
                        )
                );
        assertThat(execution.get().getCountryDeployments())
                .hasSize(1)
                .extracting(
                        "country",
                        "platform",
                        "jobUrl",
                        "jobLink",
                        "status",
                        "result",
                        "startDateTime",
                        "estimatedDuration",
                        "duration"
                )
                .containsOnly(
                        tuple(
                                esCountry,
                                "integration-1",
                                "http://build.fr/execution/es",
                                "/execution/path/to/folder/es",
                                JobStatus.DONE,
                                Result.SUCCESS,
                                new Date(1590200515000L),
                                300L,
                                200L
                        )
                );
        verify(executionCompletionRequestRepository, never()).delete(any(ExecutionCompletionRequest.class));
        verify(qualityService).computeQuality(execution.get());
        verify(scenariosIndexerStrategy, never()).getScenariosIndexer(Technology.CUCUMBER);
        verify(scenariosIndexerStrategy).getScenariosIndexer(Technology.POSTMAN);
    }

    @Test
    public void getExecution_returnExecution_whenCycleDefinitionFoundAndCountryFoundAndTypeNotInDB() throws IOException {
        // Given
        PlannedIndexation plannedIndexation = mock(PlannedIndexation.class);
        File executionFile = mock(File.class);
        CycleDefinition cycleDefinition = mock(CycleDefinition.class);

        File executionBuildInformationFile = mock(File.class);

        Build executionBuild = mock(Build.class);

        Execution previousExecution = mock(Execution.class);

        File cycleDefinitionFile = mock(File.class);

        CycleDef cycleDef = mock(CycleDef.class);

        Map<String, QualityThreshold> qualityThresholds = mock(Map.class);

        Country frCountry = mock(Country.class);
        Country esCountry = mock(Country.class);
        Country deCountry = mock(Country.class);

        Type apiType = mock(Type.class);
        Source apiSource = mock(Source.class);
        Type mobileType = mock(Type.class);
        Source mobileSource = mock(Source.class);
        Type anotherType = mock(Type.class);

        File esFolder = mock(File.class);
        File frFolder = mock(File.class);

        File apiTypeFolder = mock(File.class);
        File desktopTypeFolder = mock(File.class);
        File mobileTypeFolder = mock(File.class);

        PlatformRule platformRule11 = mock(PlatformRule.class);
        PlatformRule platformRule12 = mock(PlatformRule.class);
        PlatformRule platformRule21 = mock(PlatformRule.class);

        Map<String, List<PlatformRule>> platformRules = new HashMap<String, List<PlatformRule>>(){{
            put("integration-1", Arrays.asList(platformRule11, platformRule12));
            put("integration-2", Arrays.asList(platformRule21));
        }};

        File esBuildFile = mock(File.class);
        File frBuildFile = mock(File.class);

        File apiBuildFile = mock(File.class);
        File mobileBuildFile = mock(File.class);

        Build esBuild = mock(Build.class);
        Build frBuild = mock(Build.class);

        Build apiBuild = mock(Build.class);
        Build mobileBuild = mock(Build.class);

        // When
        when(plannedIndexation.getExecutionFolder()).thenReturn(executionFile);
        when(executionFile.listFiles()).thenReturn(
                new File[] {
                        executionBuildInformationFile,
                        cycleDefinitionFile,
                        esFolder,
                        frFolder
                });
        when(executionBuildInformationFile.isFile()).thenReturn(true);
        when(executionBuildInformationFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(plannedIndexation.getCycleDefinition()).thenReturn(cycleDefinition);
        when(cycleDefinition.getProjectId()).thenReturn(1L);
        when(cycleDefinition.getBranch()).thenReturn("develop");
        when(cycleDefinition.getName()).thenReturn("day");

        when(objectMapper.readValue(executionBuildInformationFile, Build.class)).thenReturn(executionBuild);
        when(executionBuild.getLink()).thenReturn("/execution/path/to/folder");
        when(executionBuild.getUrl()).thenReturn("http://build.fr/execution");
        when(executionBuild.getResult()).thenReturn(Result.SUCCESS);
        when(executionBuild.getRelease()).thenReturn("release-1");
        when(executionBuild.getVersion()).thenReturn("v1.0");
        when(executionBuild.isBuilding()).thenReturn(false);
        when(executionBuild.getDuration()).thenReturn(150L);
        when(executionBuild.getEstimatedDuration()).thenReturn(100L);
        when(executionBuild.getTimestamp()).thenReturn(1589200515000L);
        when(executionBuild.getVersionTimestamp()).thenReturn(1594718326000L);

        when(executionRepository.findByProjectIdAndJobUrlOrJobLink(1L, "http://build.fr/execution", "/execution/path/to/folder")).thenReturn(Optional.of(previousExecution));
        when(previousExecution.getStatus()).thenReturn(JobStatus.UNAVAILABLE);
        when(previousExecution.getId()).thenReturn(1L);
        when(executionCompletionRequestRepository.findById("http://build.fr/execution")).thenReturn(Optional.empty());
        when(cycleDefinitionFile.isFile()).thenReturn(true);
        when(cycleDefinitionFile.getName()).thenReturn(CYCLE_DEFINITION_FILE_NAME);
        when(objectMapper.readValue(cycleDefinitionFile, CycleDef.class)).thenReturn(cycleDef);
        when(cycleDef.isBlockingValidation()).thenReturn(true);
        when(cycleDef.getPlatformsRules()).thenReturn(platformRules);
        when(cycleDef.getQualityThresholds()).thenReturn(qualityThresholds);
        when(objectMapper.writeValueAsString(qualityThresholds)).thenReturn("final-quality-threshold");

        when(platformRule11.isEnabled()).thenReturn(true);
        when(platformRule11.getCountry()).thenReturn("es");
        when(platformRule11.getTestTypes()).thenReturn("api");
        when(platformRule11.getCountryTags()).thenReturn("all-countries");
        when(platformRule11.getSeverityTags()).thenReturn("all-severities");
        when(platformRule11.isBlockingValidation()).thenReturn(true);
        when(platformRule12.isEnabled()).thenReturn(false);
        when(platformRule21.isEnabled()).thenReturn(true);
        when(platformRule21.getCountry()).thenReturn("fr");
        when(platformRule21.getTestTypes()).thenReturn("mobile,desktop");
        when(platformRule21.getCountryTags()).thenReturn("fr");
        when(platformRule21.getSeverityTags()).thenReturn("sanity");
        when(platformRule21.isBlockingValidation()).thenReturn(false);

        when(frCountry.getCode()).thenReturn("fr");
        when(esCountry.getCode()).thenReturn("es");

        when(apiType.getCode()).thenReturn("api");
        when(apiType.getSource()).thenReturn(apiSource);
        when(apiSource.getTechnology()).thenReturn(Technology.POSTMAN);
        when(mobileType.getCode()).thenReturn("mobile");
        when(mobileType.getSource()).thenReturn(mobileSource);
        when(mobileSource.getTechnology()).thenReturn(Technology.CUCUMBER);
        when(anotherType.getCode()).thenReturn("another-type");

        when(countryRepository.findAllByProjectIdOrderByCode(1L)).thenReturn(Arrays.asList(frCountry, esCountry, deCountry));
        when(typeRepository.findAllByProjectIdOrderByCode(1L)).thenReturn(Arrays.asList(apiType, mobileType, anotherType));

        when(esBuildFile.isFile()).thenReturn(true);
        when(esBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(esBuildFile, Build.class)).thenReturn(esBuild);
        when(esBuild.getLink()).thenReturn("/execution/path/to/folder/es");
        when(esBuild.getUrl()).thenReturn("http://build.fr/execution/es");
        when(esBuild.getResult()).thenReturn(Result.SUCCESS);
        when(esBuild.isBuilding()).thenReturn(true);
        when(esBuild.getDuration()).thenReturn(200L);
        when(esBuild.getEstimatedDuration()).thenReturn(300L);
        when(esBuild.getTimestamp()).thenReturn(1590200515000L);

        when(frBuildFile.isFile()).thenReturn(true);
        when(frBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(frBuildFile, Build.class)).thenReturn(frBuild);
        when(frBuild.getLink()).thenReturn("/execution/path/to/folder/fr");
        when(frBuild.getUrl()).thenReturn("http://build.fr/execution/fr");
        when(frBuild.getResult()).thenReturn(Result.UNSTABLE);
        when(frBuild.isBuilding()).thenReturn(false);
        when(frBuild.getDuration()).thenReturn(500L);
        when(frBuild.getEstimatedDuration()).thenReturn(99L);
        when(frBuild.getTimestamp()).thenReturn(1589211515000L);

        when(esFolder.isDirectory()).thenReturn(true);
        when(esFolder.getName()).thenReturn("es");
        when(esFolder.listFiles()).thenReturn(new File[]{apiTypeFolder, esBuildFile});
        when(frFolder.isDirectory()).thenReturn(true);
        when(frFolder.getName()).thenReturn("fr");
        when(frFolder.listFiles()).thenReturn(new File[]{desktopTypeFolder, mobileTypeFolder, frBuildFile});

        when(apiBuildFile.isFile()).thenReturn(true);
        when(apiBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(apiBuildFile, Build.class)).thenReturn(apiBuild);
        when(apiBuild.getLink()).thenReturn("/execution/path/to/folder/api");
        when(apiBuild.getUrl()).thenReturn("http://build.fr/execution/api");
        when(apiBuild.isBuilding()).thenReturn(true);
        when(apiBuild.getDuration()).thenReturn(650L);
        when(apiBuild.getEstimatedDuration()).thenReturn(185L);
        when(apiBuild.getTimestamp()).thenReturn(1666200515000L);
        when(apiBuild.getComment()).thenReturn("comment for api build");

        when(mobileBuildFile.isFile()).thenReturn(true);
        when(mobileBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(mobileBuildFile, Build.class)).thenReturn(mobileBuild);
        when(mobileBuild.getLink()).thenReturn("/execution/path/to/folder/mobile");
        when(mobileBuild.getUrl()).thenReturn("http://build.fr/execution/mobile");
        when(mobileBuild.isBuilding()).thenReturn(true);
        when(mobileBuild.getDuration()).thenReturn(75L);
        when(mobileBuild.getEstimatedDuration()).thenReturn(3050L);
        when(mobileBuild.getTimestamp()).thenReturn(1512345515000L);
        when(mobileBuild.getComment()).thenReturn("comment for mobile build");

        when(apiTypeFolder.isDirectory()).thenReturn(true);
        when(apiTypeFolder.getName()).thenReturn("api");
        when(apiTypeFolder.listFiles()).thenReturn(new File[]{apiBuildFile});
        when(desktopTypeFolder.isDirectory()).thenReturn(true);
        when(desktopTypeFolder.getName()).thenReturn("desktop");
        when(mobileTypeFolder.isDirectory()).thenReturn(true);
        when(mobileTypeFolder.getName()).thenReturn("mobile");
        when(mobileTypeFolder.listFiles()).thenReturn(new File[]{mobileBuildFile});

        // Then
        Optional<Execution> execution = cut.getExecution(plannedIndexation);
        assertThat(execution).isNotEmpty();
        assertThat(execution.get().getBranch()).isEqualTo("develop");
        assertThat(execution.get().getName()).isEqualTo("day");
        assertThat(execution.get().getRelease()).isEqualTo("release-1");
        assertThat(execution.get().getVersion()).isEqualTo("v1.0");
        assertThat(execution.get().getBuildDateTime()).isEqualTo(new Date(1594718326000L));
        assertThat(execution.get().getTestDateTime()).isEqualTo(new Date(1589200515000L));
        assertThat(execution.get().getJobUrl()).isEqualTo("http://build.fr/execution");
        assertThat(execution.get().getJobLink()).isEqualTo("/execution/path/to/folder");
        assertThat(execution.get().getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(execution.get().getResult()).isEqualTo(Result.SUCCESS);
        assertThat(execution.get().getAcceptance()).isEqualTo(ExecutionAcceptance.NEW);
        assertThat(execution.get().getDiscardReason()).isNull();
        assertThat(execution.get().getCycleDefinition()).isEqualTo(cycleDefinition);
        assertThat(execution.get().getBlockingValidation()).isEqualTo(true);
        assertThat(execution.get().getQualityThresholds()).isEqualTo("final-quality-threshold");
        assertThat(execution.get().getQualityStatus()).isEqualTo(QualityStatus.INCOMPLETE);
        assertThat(execution.get().getQualitySeverities()).isNull();
        assertThat(execution.get().getDuration()).isEqualTo(150L);
        assertThat(execution.get().getEstimatedDuration()).isEqualTo(100L);
        assertThat(execution.get().getRuns()).hasSize(2)
                .extracting(
                        "country",
                        "type",
                        "comment",
                        "platform",
                        "jobUrl",
                        "jobLink",
                        "status",
                        "countryTags",
                        "startDateTime",
                        "estimatedDuration",
                        "duration",
                        "severityTags",
                        "includeInThresholds"
                )
                .containsOnly(
                        tuple(
                                esCountry,
                                apiType,
                                "comment for api build",
                                "integration-1",
                                "http://build.fr/execution/api",
                                "/execution/path/to/folder/api",
                                JobStatus.DONE,
                                "all-countries",
                                new Date(1666200515000L),
                                185L,
                                650L,
                                "all-severities",
                                true
                        ),
                        tuple(
                                frCountry,
                                mobileType,
                                "comment for mobile build",
                                "integration-2",
                                "http://build.fr/execution/mobile",
                                "/execution/path/to/folder/mobile",
                                JobStatus.DONE,
                                "fr",
                                new Date(1512345515000L),
                                3050L,
                                75L,
                                "sanity",
                                false
                        )
                );
        assertThat(execution.get().getCountryDeployments())
                .hasSize(2)
                .extracting(
                        "country",
                        "platform",
                        "jobUrl",
                        "jobLink",
                        "status",
                        "result",
                        "startDateTime",
                        "estimatedDuration",
                        "duration"
                )
                .containsOnly(
                        tuple(
                                esCountry,
                                "integration-1",
                                "http://build.fr/execution/es",
                                "/execution/path/to/folder/es",
                                JobStatus.DONE,
                                Result.SUCCESS,
                                new Date(1590200515000L),
                                300L,
                                200L
                        ),
                        tuple(
                                frCountry,
                                "integration-2",
                                "http://build.fr/execution/fr",
                                "/execution/path/to/folder/fr",
                                JobStatus.DONE,
                                Result.UNSTABLE,
                                new Date(1589211515000L),
                                99L,
                                500L

                        )
                );
        verify(executionCompletionRequestRepository, never()).delete(any(ExecutionCompletionRequest.class));
        verify(qualityService).computeQuality(execution.get());
        verify(scenariosIndexerStrategy).getScenariosIndexer(Technology.CUCUMBER);
        verify(scenariosIndexerStrategy).getScenariosIndexer(Technology.POSTMAN);
    }

    @Test
    public void getExecution_returnExecution_whenCycleDefinitionFoundAndCountryFolderFoundButTypeFolderNotFound() throws IOException {
        // Given
        PlannedIndexation plannedIndexation = mock(PlannedIndexation.class);
        File executionFile = mock(File.class);
        CycleDefinition cycleDefinition = mock(CycleDefinition.class);

        File executionBuildInformationFile = mock(File.class);

        Build executionBuild = mock(Build.class);

        Execution previousExecution = mock(Execution.class);

        File cycleDefinitionFile = mock(File.class);

        CycleDef cycleDef = mock(CycleDef.class);

        Map<String, QualityThreshold> qualityThresholds = mock(Map.class);

        Country frCountry = mock(Country.class);
        Country esCountry = mock(Country.class);
        Country deCountry = mock(Country.class);

        Type apiType = mock(Type.class);
        Source apiSource = mock(Source.class);
        Type desktopType = mock(Type.class);
        Source desktopSource = mock(Source.class);
        Type mobileType = mock(Type.class);
        Source mobileSource = mock(Source.class);
        Type anotherType = mock(Type.class);

        File esFolder = mock(File.class);
        File frFolder = mock(File.class);

        File apiTypeFolder = mock(File.class);
        File mobileTypeFolder = mock(File.class);

        PlatformRule platformRule11 = mock(PlatformRule.class);
        PlatformRule platformRule12 = mock(PlatformRule.class);
        PlatformRule platformRule21 = mock(PlatformRule.class);

        Map<String, List<PlatformRule>> platformRules = new HashMap<String, List<PlatformRule>>(){{
            put("integration-1", Arrays.asList(platformRule11, platformRule12));
            put("integration-2", Arrays.asList(platformRule21));
        }};

        File esBuildFile = mock(File.class);
        File frBuildFile = mock(File.class);

        File apiBuildFile = mock(File.class);
        File mobileBuildFile = mock(File.class);

        Build esBuild = mock(Build.class);
        Build frBuild = mock(Build.class);

        Build apiBuild = mock(Build.class);
        Build mobileBuild = mock(Build.class);

        // When
        when(plannedIndexation.getExecutionFolder()).thenReturn(executionFile);
        when(executionFile.listFiles()).thenReturn(
                new File[] {
                        executionBuildInformationFile,
                        cycleDefinitionFile,
                        esFolder,
                        frFolder
                });
        when(executionBuildInformationFile.isFile()).thenReturn(true);
        when(executionBuildInformationFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(plannedIndexation.getCycleDefinition()).thenReturn(cycleDefinition);
        when(cycleDefinition.getProjectId()).thenReturn(1L);
        when(cycleDefinition.getBranch()).thenReturn("develop");
        when(cycleDefinition.getName()).thenReturn("day");

        when(objectMapper.readValue(executionBuildInformationFile, Build.class)).thenReturn(executionBuild);
        when(executionBuild.getLink()).thenReturn("/execution/path/to/folder");
        when(executionBuild.getUrl()).thenReturn("http://build.fr/execution");
        when(executionBuild.getResult()).thenReturn(Result.SUCCESS);
        when(executionBuild.getRelease()).thenReturn("release-1");
        when(executionBuild.getVersion()).thenReturn("v1.0");
        when(executionBuild.isBuilding()).thenReturn(false);
        when(executionBuild.getDuration()).thenReturn(150L);
        when(executionBuild.getEstimatedDuration()).thenReturn(100L);
        when(executionBuild.getTimestamp()).thenReturn(1589200515000L);
        when(executionBuild.getVersionTimestamp()).thenReturn(1594718326000L);

        when(executionRepository.findByProjectIdAndJobUrlOrJobLink(1L, "http://build.fr/execution", "/execution/path/to/folder")).thenReturn(Optional.of(previousExecution));
        when(previousExecution.getStatus()).thenReturn(JobStatus.UNAVAILABLE);
        when(previousExecution.getId()).thenReturn(1L);
        when(executionCompletionRequestRepository.findById("http://build.fr/execution")).thenReturn(Optional.empty());
        when(cycleDefinitionFile.isFile()).thenReturn(true);
        when(cycleDefinitionFile.getName()).thenReturn(CYCLE_DEFINITION_FILE_NAME);
        when(objectMapper.readValue(cycleDefinitionFile, CycleDef.class)).thenReturn(cycleDef);
        when(cycleDef.isBlockingValidation()).thenReturn(true);
        when(cycleDef.getPlatformsRules()).thenReturn(platformRules);
        when(cycleDef.getQualityThresholds()).thenReturn(qualityThresholds);
        when(objectMapper.writeValueAsString(qualityThresholds)).thenReturn("final-quality-threshold");

        when(platformRule11.isEnabled()).thenReturn(true);
        when(platformRule11.getCountry()).thenReturn("es");
        when(platformRule11.getTestTypes()).thenReturn("api");
        when(platformRule11.getCountryTags()).thenReturn("all-countries");
        when(platformRule11.getSeverityTags()).thenReturn("all-severities");
        when(platformRule11.isBlockingValidation()).thenReturn(true);
        when(platformRule12.isEnabled()).thenReturn(false);
        when(platformRule21.isEnabled()).thenReturn(true);
        when(platformRule21.getCountry()).thenReturn("fr");
        when(platformRule21.getTestTypes()).thenReturn("mobile,desktop");
        when(platformRule21.getCountryTags()).thenReturn("fr");
        when(platformRule21.getSeverityTags()).thenReturn("sanity");
        when(platformRule21.isBlockingValidation()).thenReturn(false);

        when(frCountry.getCode()).thenReturn("fr");
        when(esCountry.getCode()).thenReturn("es");

        when(apiType.getCode()).thenReturn("api");
        when(apiType.getSource()).thenReturn(apiSource);
        when(apiSource.getTechnology()).thenReturn(Technology.POSTMAN);
        when(desktopType.getCode()).thenReturn("desktop");
        when(desktopType.getSource()).thenReturn(desktopSource);
        when(mobileType.getCode()).thenReturn("mobile");
        when(mobileType.getSource()).thenReturn(mobileSource);
        when(mobileSource.getTechnology()).thenReturn(Technology.CUCUMBER);

        when(countryRepository.findAllByProjectIdOrderByCode(1L)).thenReturn(Arrays.asList(frCountry, esCountry, deCountry));
        when(typeRepository.findAllByProjectIdOrderByCode(1L)).thenReturn(Arrays.asList(apiType, desktopType, mobileType, anotherType));

        when(esBuildFile.isFile()).thenReturn(true);
        when(esBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(esBuildFile, Build.class)).thenReturn(esBuild);
        when(esBuild.getLink()).thenReturn("/execution/path/to/folder/es");
        when(esBuild.getUrl()).thenReturn("http://build.fr/execution/es");
        when(esBuild.getResult()).thenReturn(Result.SUCCESS);
        when(esBuild.isBuilding()).thenReturn(true);
        when(esBuild.getDuration()).thenReturn(200L);
        when(esBuild.getEstimatedDuration()).thenReturn(300L);
        when(esBuild.getTimestamp()).thenReturn(1590200515000L);

        when(frBuildFile.isFile()).thenReturn(true);
        when(frBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(frBuildFile, Build.class)).thenReturn(frBuild);
        when(frBuild.getLink()).thenReturn("/execution/path/to/folder/fr");
        when(frBuild.getUrl()).thenReturn("http://build.fr/execution/fr");
        when(frBuild.getResult()).thenReturn(Result.UNSTABLE);
        when(frBuild.isBuilding()).thenReturn(false);
        when(frBuild.getDuration()).thenReturn(500L);
        when(frBuild.getEstimatedDuration()).thenReturn(99L);
        when(frBuild.getTimestamp()).thenReturn(1589211515000L);

        when(esFolder.isDirectory()).thenReturn(true);
        when(esFolder.getName()).thenReturn("es");
        when(esFolder.listFiles()).thenReturn(new File[]{apiTypeFolder, esBuildFile});
        when(frFolder.isDirectory()).thenReturn(true);
        when(frFolder.getName()).thenReturn("fr");
        when(frFolder.listFiles()).thenReturn(new File[]{mobileTypeFolder, frBuildFile});

        when(apiBuildFile.isFile()).thenReturn(true);
        when(apiBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(apiBuildFile, Build.class)).thenReturn(apiBuild);
        when(apiBuild.getLink()).thenReturn("/execution/path/to/folder/api");
        when(apiBuild.getUrl()).thenReturn("http://build.fr/execution/api");
        when(apiBuild.isBuilding()).thenReturn(true);
        when(apiBuild.getDuration()).thenReturn(650L);
        when(apiBuild.getEstimatedDuration()).thenReturn(185L);
        when(apiBuild.getTimestamp()).thenReturn(1666200515000L);
        when(apiBuild.getComment()).thenReturn("comment for api build");

        when(mobileBuildFile.isFile()).thenReturn(true);
        when(mobileBuildFile.getName()).thenReturn(BUILD_INFORMATION_FILE_NAME);
        when(objectMapper.readValue(mobileBuildFile, Build.class)).thenReturn(mobileBuild);
        when(mobileBuild.getLink()).thenReturn("/execution/path/to/folder/mobile");
        when(mobileBuild.getUrl()).thenReturn("http://build.fr/execution/mobile");
        when(mobileBuild.isBuilding()).thenReturn(true);
        when(mobileBuild.getDuration()).thenReturn(75L);
        when(mobileBuild.getEstimatedDuration()).thenReturn(3050L);
        when(mobileBuild.getTimestamp()).thenReturn(1512345515000L);
        when(mobileBuild.getComment()).thenReturn("comment for mobile build");

        when(apiTypeFolder.isDirectory()).thenReturn(true);
        when(apiTypeFolder.getName()).thenReturn("api");
        when(apiTypeFolder.listFiles()).thenReturn(new File[]{apiBuildFile});
        when(mobileTypeFolder.isDirectory()).thenReturn(true);
        when(mobileTypeFolder.getName()).thenReturn("mobile");
        when(mobileTypeFolder.listFiles()).thenReturn(new File[]{mobileBuildFile});

        // Then
        Optional<Execution> execution = cut.getExecution(plannedIndexation);
        assertThat(execution).isNotEmpty();
        assertThat(execution.get().getBranch()).isEqualTo("develop");
        assertThat(execution.get().getName()).isEqualTo("day");
        assertThat(execution.get().getRelease()).isEqualTo("release-1");
        assertThat(execution.get().getVersion()).isEqualTo("v1.0");
        assertThat(execution.get().getBuildDateTime()).isEqualTo(new Date(1594718326000L));
        assertThat(execution.get().getTestDateTime()).isEqualTo(new Date(1589200515000L));
        assertThat(execution.get().getJobUrl()).isEqualTo("http://build.fr/execution");
        assertThat(execution.get().getJobLink()).isEqualTo("/execution/path/to/folder");
        assertThat(execution.get().getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(execution.get().getResult()).isEqualTo(Result.SUCCESS);
        assertThat(execution.get().getAcceptance()).isEqualTo(ExecutionAcceptance.NEW);
        assertThat(execution.get().getDiscardReason()).isNull();
        assertThat(execution.get().getCycleDefinition()).isEqualTo(cycleDefinition);
        assertThat(execution.get().getBlockingValidation()).isEqualTo(true);
        assertThat(execution.get().getQualityThresholds()).isEqualTo("final-quality-threshold");
        assertThat(execution.get().getQualityStatus()).isEqualTo(QualityStatus.INCOMPLETE);
        assertThat(execution.get().getQualitySeverities()).isNull();
        assertThat(execution.get().getDuration()).isEqualTo(150L);
        assertThat(execution.get().getEstimatedDuration()).isEqualTo(100L);
        assertThat(execution.get().getRuns()).hasSize(2)
                .extracting(
                        "country",
                        "type",
                        "comment",
                        "platform",
                        "jobUrl",
                        "jobLink",
                        "status",
                        "countryTags",
                        "startDateTime",
                        "estimatedDuration",
                        "duration",
                        "severityTags",
                        "includeInThresholds"
                )
                .containsOnly(
                        tuple(
                                esCountry,
                                apiType,
                                "comment for api build",
                                "integration-1",
                                "http://build.fr/execution/api",
                                "/execution/path/to/folder/api",
                                JobStatus.DONE,
                                "all-countries",
                                new Date(1666200515000L),
                                185L,
                                650L,
                                "all-severities",
                                true
                        ),
                        tuple(
                                frCountry,
                                mobileType,
                                "comment for mobile build",
                                "integration-2",
                                "http://build.fr/execution/mobile",
                                "/execution/path/to/folder/mobile",
                                JobStatus.DONE,
                                "fr",
                                new Date(1512345515000L),
                                3050L,
                                75L,
                                "sanity",
                                false
                        )
                );
        assertThat(execution.get().getCountryDeployments())
                .hasSize(2)
                .extracting(
                        "country",
                        "platform",
                        "jobUrl",
                        "jobLink",
                        "status",
                        "result",
                        "startDateTime",
                        "estimatedDuration",
                        "duration"
                )
                .containsOnly(
                        tuple(
                                esCountry,
                                "integration-1",
                                "http://build.fr/execution/es",
                                "/execution/path/to/folder/es",
                                JobStatus.DONE,
                                Result.SUCCESS,
                                new Date(1590200515000L),
                                300L,
                                200L
                        ),
                        tuple(
                                frCountry,
                                "integration-2",
                                "http://build.fr/execution/fr",
                                "/execution/path/to/folder/fr",
                                JobStatus.DONE,
                                Result.UNSTABLE,
                                new Date(1589211515000L),
                                99L,
                                500L

                        )
                );
        verify(executionCompletionRequestRepository, never()).delete(any(ExecutionCompletionRequest.class));
        verify(qualityService).computeQuality(execution.get());
        verify(scenariosIndexerStrategy).getScenariosIndexer(Technology.CUCUMBER);
        verify(scenariosIndexerStrategy).getScenariosIndexer(Technology.POSTMAN);
    }

}
