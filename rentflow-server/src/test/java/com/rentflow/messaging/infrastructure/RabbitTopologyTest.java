package com.rentflow.messaging.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitTopologyTest {
    @Test
    void bindsStoreOrderEventsToDedicatedDurableQueueAndDeadLetterQueue() {
        RabbitTopology topology = new RabbitTopology();
        TopicExchange domainExchange = topology.domainExchange();
        TopicExchange deadLetterExchange = topology.deadLetterExchange();
        Queue queue = topology.storeOrderQueue();
        Queue deadQueue = topology.storeOrderDeadQueue();

        Binding binding = topology.storeOrderBinding(queue, domainExchange);
        Binding deadBinding = topology.storeOrderDeadBinding(deadQueue, deadLetterExchange);

        assertThat(queue.getName()).isEqualTo(RabbitTopology.STORE_ORDER_QUEUE);
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitTopology.DEAD_LETTER_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", "store.order.dead");
        assertThat(binding.getExchange()).isEqualTo(RabbitTopology.DOMAIN_EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo("store.order.#");
        assertThat(deadQueue.getName()).isEqualTo(RabbitTopology.STORE_ORDER_DEAD_QUEUE);
        assertThat(deadBinding.getExchange()).isEqualTo(RabbitTopology.DEAD_LETTER_EXCHANGE);
        assertThat(deadBinding.getRoutingKey()).isEqualTo("store.order.dead");
    }
}
