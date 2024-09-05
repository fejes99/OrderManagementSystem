package se.david.microservices.core.shipping.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import se.david.api.core.shipping.dto.ShippingDto;
import se.david.api.core.shipping.service.ShippingService;
import se.david.util.http.ServiceUtil;

import java.util.List;

@RestController
public class ShippingServiceImpl implements ShippingService {
  private final ServiceUtil serviceUtil;

  @Autowired
  public ShippingServiceImpl(ServiceUtil serviceUtil) {
    this.serviceUtil = serviceUtil;
  }

  @Override
  public List<ShippingDto> getShipments() {
    return List.of();
  }

  @Override
  public ShippingDto getShipping(int orderId) {
    return null;
  }

  @Override
  public ShippingDto createShippingOrder(ShippingDto shipping) {
    return null;
  }

  @Override
  public ShippingDto updateShippingStatus(int orderId, String status) {
    return null;
  }

  @Override
  public void deleteShipping(int shippingId) {

  }
}
