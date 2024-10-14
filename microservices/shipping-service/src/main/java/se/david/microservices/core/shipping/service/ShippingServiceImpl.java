package se.david.microservices.core.shipping.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
import java.util.logging.Level;

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
  public Flux<ShippingDto> getShipments() {
    LOG.info("getShipments: Fetching all shipments");

    return repository.findAll()
      .map(this::mapToShippingDtoWithServiceAddress)
      .doOnError(ex -> LOG.error("Error fetching shipments", ex))
      .log(LOG.getName(), Level.FINE);
  }

  private ShippingDto mapToShippingDtoWithServiceAddress(Shipping shipping) {
    return new ShippingDto(shipping.getOrderId(), shipping.getShippingAddress(), shipping.getStatus(), serviceUtil.getServiceAddress());
  }

  @Override
  public Flux<ShippingDto> getShipmentsByOrderIds(List<Integer> orderIds) {
    LOG.info("getShipmentsByOrderIds: Fetching all shipments by list of orderIds");

    return repository.findByOrderIdIn(orderIds)
      .map(this::mapToShippingDtoWithServiceAddress)
      .doOnError(ex -> LOG.error("Error fetching shipments by orderIds", ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<ShippingDto> getShippingByOrderId(int orderId) {
    LOG.debug("getShipping: Search shipping for orderId: {}", orderId);
    validateOrderId(orderId);

    return findShippingByOrderId(orderId)
      .map(this::mapToShippingDtoWithServiceAddress)
      .doOnError(ex -> LOG.error("Error fetching shipping for orderId: {}", orderId, ex))
      .log(LOG.getName(), Level.FINE);
  }

  private Mono<Shipping> findShippingByOrderId(int orderId) {
    return repository.findByOrderId(orderId)
      .switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + orderId)))
      .log(LOG.getName(), Level.FINE);
  }

  private void validateOrderId(int orderId) {
    if(orderId < 1) {
      throw new InvalidInputException("Invalid orderId: " + orderId);
    }
  }

  @Override
  public Mono<ShippingDto> createShippingOrder(ShippingCreateDto shippingCreateDto) {
    LOG.debug("createShippingOrder: Creating shipping for orderId: {}", shippingCreateDto.orderId());

    Shipping shipping = mapper.createDtoToEntity(shippingCreateDto);

    return repository.save(shipping)
      .map(this::mapToShippingDtoWithServiceAddress)
      .onErrorMap(DuplicateKeyException.class, ex ->
        new InvalidInputException("Duplicate key for orderId: " + shippingCreateDto.orderId()))
      .doOnSuccess(savedShipping -> LOG.debug("Successfully created shipping for orderId: {}", savedShipping.orderId()))
      .doOnError(ex -> LOG.error("Error creating shipping for orderId: {}", shippingCreateDto.orderId(), ex))
      .log(LOG.getName(), Level.FINE);
  }

  @Override
  public Mono<ShippingDto> updateShippingStatusByOrderId(int orderId, String status) {
    validateOrderId(orderId);
    LOG.debug("updateShippingStatus: Updating shipping status to: {}", status);

    return findShippingByOrderId(orderId)
      .flatMap(shipping -> {
        shipping.setStatus(status);
        return repository.save(shipping);
      })
      .map(this::mapToShippingDtoWithServiceAddress)
      .onErrorMap(IllegalArgumentException.class, ex ->
        new InvalidInputException("Invalid orderId: " + orderId))
      .doOnSuccess(updatedShipping -> LOG.debug("Successfully updated shipping status for orderId: {}", updatedShipping.orderId()))
      .doOnError(ex -> LOG.error("Error updating shipping status for orderId: {}", orderId, ex))
      .log(LOG.getName(), Level.FINE);
  }

}
