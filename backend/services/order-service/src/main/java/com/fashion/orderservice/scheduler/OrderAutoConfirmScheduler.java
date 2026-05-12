package com.fashion.orderservice.scheduler;

import com.fashion.orderservice.entity.Order;
import com.fashion.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that auto-confirms COD orders after the 15-minute grace period.
 *
 * <p>Runs every 60 seconds, finds all COD orders that are still PENDING,
 * have their inventory reserved, and were created more than 15 minutes ago,
 * then transitions them to CONFIRMED + PAID status.</p>
 *
 * <p>This design gives customers a window to cancel COD orders before
 * the system locks them in.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAutoConfirmScheduler {

    /** Must match the grace period constant used in OrderServiceImpl. */
    private static final long GRACE_PERIOD_MINUTES = 15;

    private final OrderRepository orderRepository;

    /**
     * Runs every 60 seconds to auto-confirm eligible COD orders.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void autoConfirmCodOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(GRACE_PERIOD_MINUTES);
        List<Order> eligibleOrders = orderRepository.findCodOrdersReadyForAutoConfirm(cutoff);

        if (eligibleOrders.isEmpty()) {
            return;
        }

        log.info("Auto-confirming {} COD order(s) past {}-minute grace period",
                eligibleOrders.size(), GRACE_PERIOD_MINUTES);

        for (Order order : eligibleOrders) {
            order.setStatus(Order.OrderStatus.CONFIRMED);
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            orderRepository.save(order);

            log.info("Auto-confirmed COD orderId={} orderNumber={}",
                    order.getId(), order.getOrderNumber());
        }
    }
}
