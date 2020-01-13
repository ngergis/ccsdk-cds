/*
 * Copyright © 2018-2019 AT&T Intellectual Property.
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

package org.onap.ccsdk.cds.blueprintsprocessor.functions.message.prioritization.service

import org.onap.ccsdk.cds.blueprintsprocessor.functions.message.prioritization.MessagePrioritizationStateService
import org.onap.ccsdk.cds.blueprintsprocessor.functions.message.prioritization.MessageState
import org.onap.ccsdk.cds.blueprintsprocessor.functions.message.prioritization.db.MessagePrioritization
import org.onap.ccsdk.cds.blueprintsprocessor.functions.message.prioritization.ids
import org.onap.ccsdk.cds.blueprintsprocessor.functions.message.prioritization.kafka.DefaultMessagePrioritizeProcessor
import org.onap.ccsdk.cds.blueprintsprocessor.functions.message.prioritization.orderByHighestPriority
import org.onap.ccsdk.cds.controllerblueprints.core.logger

/** Sample Prioritization Service, Define spring service injector to register in application*/
open class SampleMessagePrioritizationService(private val messagePrioritizationStateService: MessagePrioritizationStateService) :
    AbstractMessagePrioritizationService(messagePrioritizationStateService) {

    private val log = logger(DefaultMessagePrioritizeProcessor::class)

    /** Child overriding this implementation , if necessary */
    override suspend fun handleAggregation(messages: List<MessagePrioritization>) {
        log.info("messages(${messages.ids()}) aggregated")
        /** Sequence based on Priority and Updated Date */
        val sequencedMessage = messages.orderByHighestPriority()
        /** Update all messages to aggregated state */
        messagePrioritizationStateService.setMessagesState(sequencedMessage.ids(), MessageState.AGGREGATED.name)
        output(sequencedMessage)
    }

    /** If consumer wants specific correlation with respect to group and types, then populate the specific types,
     * otherwise correlation happens with group and correlationId */
    override fun getGroupCorrelationTypes(messagePrioritization: MessagePrioritization): List<String>? {
        return when (messagePrioritization.group) {
            /** Dummy Implementation, This can also be read from file and stored as cached map **/
            "group-typed" -> arrayListOf("type-0", "type-1", "type-2")
            else -> null
        }
    }
}
