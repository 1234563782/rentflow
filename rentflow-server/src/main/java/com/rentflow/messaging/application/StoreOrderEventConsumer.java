package com.rentflow.messaging.application;

import com.rentflow.messaging.infrastructure.DomainEventEnvelope;
import com.rentflow.messaging.infrastructure.OutboxMapper;
import com.rentflow.messaging.infrastructure.RabbitTopology;
import com.rentflow.notification.api.NotificationWriter;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "rentflow.messaging", name = "enabled", havingValue = "true")
public class StoreOrderEventConsumer {
    private static final String CONSUMER_NAME = "store-order-notification-materializer";
    private final OutboxMapper outboxMapper;
    private final NotificationWriter notificationWriter;

    public StoreOrderEventConsumer(OutboxMapper outboxMapper, NotificationWriter notificationWriter) {
        this.outboxMapper = outboxMapper;
        this.notificationWriter = notificationWriter;
    }

    @RabbitListener(queues = RabbitTopology.STORE_ORDER_QUEUE)
    @Transactional
    public void consume(DomainEventEnvelope event) {
        if (outboxMapper.insertInbox(CONSUMER_NAME, event.eventId()) == 0) {
            return;
        }
        NotificationSpec notification = notification(event);
        String userId = event.payload().required("userId").asText();
        String orderId = event.payload().required("orderId").asText();
        notificationWriter.createStoreOrderNotification(
                userId,
                orderId,
                event.eventType(),
                notification.type(),
                notification.title(),
                notification.content()
        );
    }

    private NotificationSpec notification(DomainEventEnvelope event) {
        return switch (event.eventType()) {
            case "store.order.created" -> new NotificationSpec(
                    "STORE_ORDER_CREATED", "订单已创建", "订单已创建，请在支付有效期内完成支付。"
            );
            case "store.order.paid" -> new NotificationSpec(
                    "STORE_ORDER_PAID", "支付成功", "订单已支付，正在等待发货。"
            );
            case "store.order.cancelled" -> new NotificationSpec(
                    "STORE_ORDER_CANCELLED", "订单已取消", "订单已取消，预占库存已经释放。"
            );
            case "store.order.shipped" -> new NotificationSpec(
                    "STORE_ORDER_SHIPPED",
                    "订单已发货",
                    "订单已由 " + event.payload().required("carrier").asText()
                            + " 发出，运单号 " + event.payload().required("trackingNumber").asText() + "。"
            );
            case "store.order.received" -> new NotificationSpec(
                    "STORE_ORDER_RECEIVED", "订单已完成", "订单已确认收货，可以评价已购商品。"
            );
            case "store.order.closed" -> new NotificationSpec(
                    "STORE_ORDER_CLOSED", "订单已关闭", "订单因超时未支付已关闭，预占库存已经释放。"
            );
            default -> throw new IllegalArgumentException("Unsupported store order event: " + event.eventType());
        };
    }

    private record NotificationSpec(String type, String title, String content) {
    }
}
