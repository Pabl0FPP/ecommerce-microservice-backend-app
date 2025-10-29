package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.helper.OrderItemMappingHelper;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.OrderItemService;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.DuplicateResourceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
	
	private final OrderItemRepository orderItemRepository;
	private final RestTemplate restTemplate;
	
	@Override
	public List<OrderItemDto> findAll() {
		log.info("*** OrderItemDto List, service; fetch all orderItems *");
		return this.orderItemRepository.findAll()
				.stream()
					.map(OrderItemMappingHelper::map)
					.map(o -> {
						o.setProductDto(this.restTemplate.getForObject(AppConstant.DiscoveredDomainsApi
								.PRODUCT_SERVICE_API_URL + "/" + o.getProductDto().getProductId(), ProductDto.class));
						o.setOrderDto(this.restTemplate.getForObject(AppConstant.DiscoveredDomainsApi
								.ORDER_SERVICE_API_URL + "/" + o.getOrderDto().getOrderId(), OrderDto.class));
						return o;
					})
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public OrderItemDto findById(final OrderItemId orderItemId) {
		log.info("*** OrderItemDto, service; fetch orderItem by id *");
	return this.orderItemRepository.findById(orderItemId)
				.map(OrderItemMappingHelper::map)
				.map(o -> {
					o.setProductDto(this.restTemplate.getForObject(AppConstant.DiscoveredDomainsApi
							.PRODUCT_SERVICE_API_URL + "/" + o.getProductDto().getProductId(), ProductDto.class));
					o.setOrderDto(this.restTemplate.getForObject(AppConstant.DiscoveredDomainsApi
							.ORDER_SERVICE_API_URL + "/" + o.getOrderDto().getOrderId(), OrderDto.class));
					return o;
				})
		.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SHIPPING_NOT_FOUND, orderItemId));
	}
	
	@Override
	public OrderItemDto save(final OrderItemDto orderItemDto) {
		log.info("*** OrderItemDto, service; save orderItem *");
		if (orderItemDto == null || orderItemDto.getOrderId() == null || orderItemDto.getProductId() == null) {
			throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD);
		}

		final OrderItemId id = OrderItemMappingHelper.toId(orderItemDto);
		if (this.orderItemRepository.existsById(id)) {
			throw new DuplicateResourceException(ErrorCode.DUPLICATE_RESOURCE, id);
		}

		try {
			return OrderItemMappingHelper.map(this.orderItemRepository
					.save(OrderItemMappingHelper.map(orderItemDto)));
		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			throw new DuplicateResourceException(ErrorCode.DUPLICATE_RESOURCE);
		}
	}
	
	@Override
	public OrderItemDto update(final OrderItemDto orderItemDto) {
		log.info("*** OrderItemDto, service; update orderItem *");
		if (orderItemDto == null || orderItemDto.getOrderId() == null || orderItemDto.getProductId() == null) {
			throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD);
		}

	final com.selimhorri.app.domain.OrderItem entity = OrderItemMappingHelper.map(orderItemDto);
	final OrderItemId entityId = OrderItemMappingHelper.toId(entity);
		if (!this.orderItemRepository.existsById(entityId)) {
			throw new ResourceNotFoundException(ErrorCode.SHIPPING_NOT_FOUND, entityId);
		}

		return OrderItemMappingHelper.map(this.orderItemRepository
				.save(entity));
	}
	
	@Override
	public void deleteById(final OrderItemId orderItemId) {
		log.info("*** Void, service; delete orderItem by id *");
		if (!this.orderItemRepository.existsById(orderItemId)) {
			throw new ResourceNotFoundException(ErrorCode.SHIPPING_NOT_FOUND, orderItemId);
		}

		this.orderItemRepository.deleteById(orderItemId);
	}
	
	
	
}









