package se.david.api.composite.order.dto;

import java.util.Date;
import java.util.List;

public record OrderAggregateDto(int orderId, int userId, int totalPrice, String status, Date createdAt,
                                ShippingSummaryDto shippingSummary, List<OrderItemSummaryDto> orderItemsSummary,
                                ServiceAddressesDto serviceAddresses) {
}
