package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.helper.FavouriteMappingHelper;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.impl.FavouriteServiceImpl;

public class FavouriteServiceImplTest {

    @Mock
    private FavouriteRepository favouriteRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FavouriteServiceImpl favouriteService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findAll_returnsMappedList() {
        LocalDateTime now = LocalDateTime.now();
        Favourite fav = Favourite.builder().userId(1).productId(2).likeDate(now).build();
        when(this.favouriteRepository.findAll()).thenReturn(List.of(fav));

        when(this.restTemplate.getForObject(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + 1, UserDto.class))
            .thenReturn(UserDto.builder().userId(1).build());
        when(this.restTemplate.getForObject(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + 2, ProductDto.class))
            .thenReturn(ProductDto.builder().productId(2).build());

        List<FavouriteDto> list = favouriteService.findAll();
        assertNotNull(list);
        assertEquals(1, list.size());
        FavouriteDto dto = list.get(0);
        assertEquals(1, dto.getUserId());
        assertEquals(2, dto.getProductId());
        assertNotNull(dto.getUserDto());
        assertNotNull(dto.getProductDto());
    }

    @Test
    void findById_notFound_throwsResourceNotFound() {
        FavouriteId id = new FavouriteId(9,9, LocalDateTime.now());
        when(this.favouriteRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> favouriteService.findById(id));
    }

    @Test
    void save_success_returnsDto() {
        LocalDateTime now = LocalDateTime.now();
        Favourite fav = Favourite.builder().userId(3).productId(4).likeDate(now).build();
        when(this.favouriteRepository.save(any(Favourite.class))).thenReturn(fav);

        FavouriteDto dto = FavouriteMappingHelper.map(fav);
        FavouriteDto result = favouriteService.save(dto);
        assertNotNull(result);
        assertEquals(3, result.getUserId());
        assertEquals(4, result.getProductId());
    }

    @Test
    void update_success_returnsDto() {
        LocalDateTime now = LocalDateTime.now();
        Favourite fav = Favourite.builder().userId(5).productId(6).likeDate(now).build();
        when(this.favouriteRepository.save(any(Favourite.class))).thenReturn(fav);
        // stub existsById to simulate existing entity
        FavouriteId id = FavouriteMappingHelper.toId(fav);
        when(this.favouriteRepository.existsById(id)).thenReturn(true);

        FavouriteDto dto = FavouriteMappingHelper.map(fav);
        FavouriteDto result = favouriteService.update(dto);
        assertNotNull(result);
        assertEquals(5, result.getUserId());
        assertEquals(6, result.getProductId());
    }

    @Test
    void deleteById_callsRepository() {
        FavouriteId id = new FavouriteId(7,8, LocalDateTime.now());
        // stub existsById to simulate existing entity so service won't throw
        when(this.favouriteRepository.existsById(id)).thenReturn(true);
        doNothing().when(this.favouriteRepository).deleteById(id);

        assertDoesNotThrow(() -> favouriteService.deleteById(id));
        verify(this.favouriteRepository).deleteById(id);
    }

}
