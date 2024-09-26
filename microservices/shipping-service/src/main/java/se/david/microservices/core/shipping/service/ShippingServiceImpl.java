package se.david.microservices.core.shipping.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import se.david.api.core.shipping.dto.ShippingCreateDto;
import se.david.api.core.shipping.dto.ShippingDto;
import se.david.api.core.shipping.service.ShippingService;
import se.david.api.exceptions.InvalidInputException;
import se.david.api.exceptions.NotFoundException;
import se.david.microservices.core.shipping.domain.entity.Shipping;
import se.david.microservices.core.shipping.domain.repository.ShippingRepository;
import se.david.microservices.core.shipping.mapper.ShippingMapper;
import se.david.util.http.ServiceUtil;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ShippingServiceImpl implements ShippingService {
  private static final Logger LOG = LoggerFactory.getLogger(ShippingServiceImpl.class);
  private final ShippingRepository repository;
  private final ServiceUtil serviceUtil;
  private final ShippingMapper mapper;

  @Autowired
  public ShippingServiceImpl(ShippingRepository repository, ServiceUtil serviceUtil, ShippingMapper mapper) {
    this.repository = repository;
    this.serviceUtil = serviceUtil;
    this.mapper = mapper;
  }

  @Override
  public List<ShippingDto> getShipments() {
    LOG.info("getShipments: Fetching all shipments");
    List<Shipping> shipments = (List<Shipping>) repository.findAll();
    return shipments.stream()
      .map(this::mapToShippingDtoWithServiceAddress)
      .collect(Collectors.toList());
  }

  private ShippingDto mapToShippingDtoWithServiceAddress(Shipping shipping) {
    return new ShippingDto(shipping.getOrderId(), shipping.getShippingAddress(), shipping.getStatus(), serviceUtil.getServiceAddress());
  }

  @Override
  public List<ShippingDto> getShipmentsByOrderIds(List<Integer> orderIds) {
    LOG.info("getShipmentsByOrderIds: Fetching all shipments by list of orderIds");

    List<Shipping> shipments = repository.findByOrderIdIn(orderIds);
    return shipments.stream()
      .map(this::mapToShippingDtoWithServiceAddress)
      .collect(Collectors.toList());
  }

  @Override
  public ShippingDto getShippingByOrderId(int orderId) {
    LOG.debug("getShipping: Search shipping for orderId: {}", orderId);
    validateOrderId(orderId);

    Shipping shipping = findShippingByOrderId(orderId);
    LOG.debug("getShipping: Found shipping for orderId: {}", orderId);
    return mapper.entityToDto(shipping);
  }

  private Shipping findShippingByOrderId(int orderId) {
    Shipping shipping = repository.findByOrderId(orderId);
    if (shipping == null) {
      throw new NotFoundException("No shipping found for orderId: " + orderId);
    }
    return shipping;
  }

  private void validateOrderId(int orderId) {
    if (orderId < 1) {
      throw new InvalidInputException("Invalid orderId: " + orderId);
    }
  }

  @Override
  public ShippingDto createShippingOrder(ShippingCreateDto shippingCreateDto) {
    LOG.debug("createShippingOrder: Creating product for orderId: {}", shippingCreateDto.orderId());

    Shipping shipping = mapper.createDtoToEntity(shippingCreateDto);
    shipping = repository.save(shipping);

    LOG.debug("createShippingOrder: Successfully created shipping for orderId: {}", shipping.getOrderId());
    return mapper.entityToDto(shipping);
  }

  @Override
  public ShippingDto updateShippingStatusByOrderId(int orderId, String status) {
    validateOrderId(orderId);
    LOG.debug("updateShippingStatus: Updating shipping status to: {}", status);

    Shipping shipping = findShippingByOrderId(orderId);
    shipping.setStatus(status);
    Shipping updatedShipping = repository.save(shipping);

    LOG.debug("updateShippingStatus: Successfully updated shipping status to: {}", status);
    return mapper.entityToDto(updatedShipping);
  }
}
