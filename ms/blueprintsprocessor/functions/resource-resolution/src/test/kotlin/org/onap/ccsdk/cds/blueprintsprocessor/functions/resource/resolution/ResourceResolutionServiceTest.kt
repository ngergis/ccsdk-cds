/*
 * Copyright © 2017-2018 AT&T Intellectual Property.
 *
 * Modifications Copyright © 2018 - 2019 IBM, Bell Canada.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onap.ccsdk.cds.blueprintsprocessor.functions.resource.resolution

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.onap.ccsdk.cds.blueprintsprocessor.core.api.data.ExecutionServiceInput
import org.onap.ccsdk.cds.blueprintsprocessor.core.utils.PayloadUtils
import org.onap.ccsdk.cds.blueprintsprocessor.functions.resource.resolution.processor.MockCapabilityScriptRA
import org.onap.ccsdk.cds.blueprintsprocessor.functions.resource.resolution.utils.ResourceAssignmentUtils
import org.onap.ccsdk.cds.controllerblueprints.core.BluePrintConstants
import org.onap.ccsdk.cds.controllerblueprints.core.BluePrintError
import org.onap.ccsdk.cds.controllerblueprints.core.BluePrintTypes
import org.onap.ccsdk.cds.controllerblueprints.core.asJsonPrimitive
import org.onap.ccsdk.cds.controllerblueprints.core.service.BluePrintContext
import org.onap.ccsdk.cds.controllerblueprints.core.utils.BluePrintMetadataUtils
import org.onap.ccsdk.cds.controllerblueprints.core.utils.JacksonUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ResourceResolutionServiceTest
 *
 * @author Brinda Santh DATE : 8/15/2018
 */
@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = [TestDatabaseConfiguration::class]
)
@TestPropertySource(locations = ["classpath:application-test.properties"])
@ComponentScan(basePackages = ["org.onap.ccsdk.cds.blueprintsprocessor", "org.onap.ccsdk.cds.controllerblueprints"])
@EnableAutoConfiguration
class ResourceResolutionServiceTest {

    private val log = LoggerFactory.getLogger(ResourceResolutionServiceTest::class.java)

    @Autowired
    lateinit var resourceResolutionService: ResourceResolutionService

    private val props = hashMapOf<String, Any>()
    private val resolutionKey = "resolutionKey"
    private val resourceId = "1"
    private val resourceType = "ServiceInstance"
    private val occurrence = 0

    @Before
    fun setup() {
        props[ResourceResolutionConstants.RESOURCE_RESOLUTION_INPUT_STORE_RESULT] = true
        props[ResourceResolutionConstants.RESOURCE_RESOLUTION_INPUT_RESOLUTION_KEY] = resolutionKey
        props[ResourceResolutionConstants.RESOURCE_RESOLUTION_INPUT_RESOURCE_ID] = resourceId
        props[ResourceResolutionConstants.RESOURCE_RESOLUTION_INPUT_RESOURCE_TYPE] = resourceType
        props[ResourceResolutionConstants.RESOURCE_RESOLUTION_INPUT_OCCURRENCE] = occurrence
    }

    @Test
    fun testRegisteredSource() {
        val sources = resourceResolutionService.registeredResourceSources()
        assertNotNull(sources, "failed to get registered sources")
        assertTrue(
            sources.containsAll(
                arrayListOf(
                    "source-input", "source-default", "source-db",
                    "source-rest", "source-capability"
                )
            ), "failed to get registered sources : $sources"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testResolveResource() {
        runBlocking {

            Assert.assertNotNull("failed to create ResourceResolutionService", resourceResolutionService)

            val bluePrintRuntimeService = BluePrintMetadataUtils.getBluePrintRuntime(
                "1234",
                "./../../../../components/model-catalog/blueprint-model/test-blueprint/baseconfiguration"
            )

            val executionServiceInput =
                JacksonUtils.readValueFromClassPathFile(
                    "payload/requests/sample-resourceresolution-request.json",
                    ExecutionServiceInput::class.java
                )!!

            val resourceAssignmentRuntimeService =
                ResourceAssignmentUtils.transformToRARuntimeService(
                    bluePrintRuntimeService,
                    "testResolveResource"
                )

            // Prepare Inputs
            PayloadUtils.prepareInputsFromWorkflowPayload(
                bluePrintRuntimeService,
                executionServiceInput.payload,
                "resource-assignment"
            )

            resourceResolutionService.resolveResources(
                resourceAssignmentRuntimeService,
                "resource-assignment",
                "baseconfig",
                props
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun testResolveResources() {
        runBlocking {
            Assert.assertNotNull("failed to create ResourceResolutionService", resourceResolutionService)

            val bluePrintRuntimeService = BluePrintMetadataUtils.getBluePrintRuntime(
                "1234",
                "./../../../../components/model-catalog/blueprint-model/test-blueprint/baseconfiguration"
            )

            val executionServiceInput =
                JacksonUtils.readValueFromClassPathFile(
                    "payload/requests/sample-resourceresolution-request.json",
                    ExecutionServiceInput::class.java
                )!!

            val artefactNames = listOf("baseconfig", "another")

            // Prepare Inputs
            PayloadUtils.prepareInputsFromWorkflowPayload(
                bluePrintRuntimeService,
                executionServiceInput.payload,
                "resource-assignment"
            )

            resourceResolutionService.resolveResources(
                bluePrintRuntimeService,
                "resource-assignment",
                artefactNames,
                props
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun testResolveResourcesWithMappingAndTemplate() {
        runBlocking {
            Assert.assertNotNull("failed to create ResourceResolutionService", resourceResolutionService)

            val bluePrintRuntimeService = BluePrintMetadataUtils.getBluePrintRuntime(
                "1234",
                "./../../../../components/model-catalog/blueprint-model/test-blueprint/baseconfiguration"
            )

            val executionServiceInput =
                JacksonUtils.readValueFromClassPathFile(
                    "payload/requests/sample-resourceresolution-request.json",
                    ExecutionServiceInput::class.java
                )!!

            val resourceAssignmentRuntimeService =
                ResourceAssignmentUtils.transformToRARuntimeService(
                    bluePrintRuntimeService,
                    "testResolveResourcesWithMappingAndTemplate"
                )

            val artifactPrefix = "another"

            // Prepare Inputs
            PayloadUtils.prepareInputsFromWorkflowPayload(
                bluePrintRuntimeService,
                executionServiceInput.payload,
                "resource-assignment"
            )

            assertNotNull(
                resourceResolutionService.resolveResources(
                    resourceAssignmentRuntimeService,
                    "resource-assignment",
                    artifactPrefix,
                    props
                ), "Couldn't Resolve Resources for artifact $artifactPrefix"
            )
        }
    }

    @Test
    fun testResolveResourcesWithResourceIdAndResourceType() {
        props[ResourceResolutionConstants.RESOURCE_RESOLUTION_INPUT_RESOLUTION_KEY] = ""
        runBlocking {
            Assert.assertNotNull("failed to create ResourceResolutionService", resourceResolutionService)

            val bluePrintRuntimeService = BluePrintMetadataUtils.getBluePrintRuntime(
                "1234",
                "./../../../../components/model-catalog/blueprint-model/test-blueprint/baseconfiguration"
            )

            val executionServiceInput =
                JacksonUtils.readValueFromClassPathFile(
                    "payload/requests/sample-resourceresolution-request.json",
                    ExecutionServiceInput::class.java
                )!!

            val resourceAssignmentRuntimeService =
                ResourceAssignmentUtils.transformToRARuntimeService(
                    bluePrintRuntimeService,
                    "testResolveResourcesWithMappingAndTemplate"
                )

            val artifactPrefix = "another"

            // Prepare Inputs
            PayloadUtils.prepareInputsFromWorkflowPayload(
                bluePrintRuntimeService,
                executionServiceInput.payload,
                "resource-assignment"
            )

            assertNotNull(
                resourceResolutionService.resolveResources(
                    resourceAssignmentRuntimeService,
                    "resource-assignment",
                    artifactPrefix,
                    props
                ), "Couldn't Resolve Resources for artifact $artifactPrefix"
            )
        }
    }

    @Test
    fun testResourceResolutionForDefinition() {
        val resourceDefinitions = BluePrintTypes.resourceDefinitions {
            resourceDefinition(name = "port-speed", description = "Port Speed") {
                property(type = "string", required = true)
                sources {
                    sourceCapability(id = "sdno", description = "SDNO Source") {
                        definedProperties {
                            type(BluePrintConstants.SCRIPT_KOTLIN)
                            scriptClassReference(MockCapabilityScriptRA::class.qualifiedName!!)
                            keyDependencies(arrayListOf("device-id"))
                        }
                    }
                    sourceDb(id = "sdnc", description = "SDNC Controller") {
                        definedProperties {
                            endpointSelector("processor-db")
                            query("SELECT PORT_SPEED FROM XXXX WHERE DEVICE_ID = :device_id")
                            inputKeyMapping {
                                map("device_id", "\$device-id")
                            }
                            keyDependencies(arrayListOf("device-id"))
                        }
                    }
                }
            }
            resourceDefinition(name = "device-id", description = "Device Id") {
                property(type = "string", required = true) {
                    sources {
                        sourceInput(id = "input", description = "Dependency Source") {}
                    }
                }
            }
        }
        runBlocking {
            val raRuntimeService = mockk<ResourceAssignmentRuntimeService>()
            every { raRuntimeService.bluePrintContext() } returns mockk<BluePrintContext>()
            every { raRuntimeService.getBluePrintError() } returns BluePrintError()
            every { raRuntimeService.setBluePrintError(any()) } returns Unit
            every { raRuntimeService.getInputValue("device-id") } returns "123456".asJsonPrimitive()
            every { raRuntimeService.putResolutionStore(any(), any()) } returns Unit

            val applicationContext = mockk<ApplicationContext>()
            every { applicationContext.getBean("rr-processor-source-capability") } returns MockCapabilityScriptRA()
            every { applicationContext.getBean("rr-processor-source-db") } returns MockCapabilityScriptRA()
            every { applicationContext.getBean("rr-processor-source-input") } returns MockCapabilityScriptRA()

            val sources = arrayListOf<String>("sdno", "sdnc")

            val resourceResolutionService = ResourceResolutionServiceImpl(applicationContext, mockk(), mockk(), mockk())
            val resolvedResources = resourceResolutionService.resolveResourceDefinition(
                raRuntimeService,
                resourceDefinitions, "port-speed", sources
            )
            assertNotNull(resolvedResources, "failed to resolve the resources")
        }
    }
}
