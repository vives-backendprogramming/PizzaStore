package be.vives.pizzastore.service;

import be.vives.pizzastore.domain.NutritionalInfo;
import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.NutritionalInfoRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.exception.ResourceNotFoundException;
import be.vives.pizzastore.mapper.PizzaMapper;
import be.vives.pizzastore.repository.CustomerRepository;
import be.vives.pizzastore.repository.OrderRepository;
import be.vives.pizzastore.repository.PizzaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure unit test for PizzaService (NO Spring context).
 * 
 * Uses Mockito's @Mock to create mocks and @InjectMocks to inject them.
 * @ExtendWith(MockitoExtension.class) is REQUIRED for this to work (even in Spring Boot 3).
 * 
 * This is DIFFERENT from @MockBean which is used with Spring test slices like @WebMvcTest.
 * - @Mock = Mockito mock (fast, no Spring)
 * - @MockBean = Spring-managed mock (slower, with Spring context)
 */
@ExtendWith(MockitoExtension.class)  // Required for @Mock and @InjectMocks
class PizzaServiceTest {

    @Mock  // Pure Mockito mock (not @MockBean!)
    private PizzaRepository pizzaRepository;

    @Mock  // Pure Mockito mock (not @MockBean!)
    private PizzaMapper pizzaMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks  // Injects the mocks above
    private PizzaService pizzaService;

    private Pizza testPizza;
    private PizzaResponse testResponse;
    private CreatePizzaRequest createRequest;

    @BeforeEach
    void setup() {
        testPizza = new Pizza("Margherita", new BigDecimal("8.50"), "Classic tomato and mozzarella");
        testPizza.setId("1");

        testResponse = new PizzaResponse("1", "Margherita", new BigDecimal("8.50"), 
                "Classic tomato and mozzarella", null, true, null);

        createRequest = new CreatePizzaRequest("Margherita", new BigDecimal("8.50"), 
                "Classic tomato and mozzarella", true, null);
    }

    @Test
    void findById_ExistingPizza_ReturnsPizzaResponse() {
        // Given
        when(pizzaRepository.findById("1")).thenReturn(Optional.of(testPizza));
        when(pizzaMapper.toResponse(testPizza)).thenReturn(testResponse);

        // When
        PizzaResponse result = pizzaService.findById("1");

        // Then
        assertThat(result.id()).isEqualTo("1");
        assertThat(result.name()).isEqualTo("Margherita");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("8.50"));

        verify(pizzaRepository).findById("1");
        verify(pizzaMapper).toResponse(testPizza);
    }

    @Test
    void findById_NonExistingPizza_ThrowsResourceNotFoundException() {
        // Given
        when(pizzaRepository.findById("999")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> pizzaService.findById("999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Pizza with id 999 not found");

        verify(pizzaRepository).findById("999");
        verify(pizzaMapper, never()).toResponse(any());
    }

    @Test
    void findByPriceLessThan_MultiplePizzas_ReturnsListOfResponses() {
        // Given
        Pizza pizza2 = new Pizza("Marinara", new BigDecimal("7.50"), "Simple");
        pizza2.setId("2");

        List<Pizza> pizzas = Arrays.asList(testPizza, pizza2);
        List<PizzaResponse> responses = Arrays.asList(
                testResponse,
                new PizzaResponse("2", "Marinara", new BigDecimal("7.50"), "Simple", null, true, null)
        );

        when(pizzaRepository.findByPriceLessThan(new BigDecimal("10.00"))).thenReturn(pizzas);
        when(pizzaMapper.toResponseList(pizzas)).thenReturn(responses);

        // When
        List<PizzaResponse> result = pizzaService.findByPriceLessThan(new BigDecimal("10.00"));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PizzaResponse::name)
                .containsExactly("Margherita", "Marinara");

        verify(pizzaRepository).findByPriceLessThan(new BigDecimal("10.00"));
        verify(pizzaMapper).toResponseList(pizzas);
    }

    @Test
    void create_ValidRequest_ReturnsSavedPizza() {
        // Given
        when(pizzaMapper.toEntity(createRequest)).thenReturn(testPizza);
        when(pizzaRepository.save(testPizza)).thenReturn(testPizza);
        when(pizzaMapper.toResponse(testPizza)).thenReturn(testResponse);

        // When
        PizzaResponse result = pizzaService.create(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Margherita");

        verify(pizzaMapper).toEntity(createRequest);
        verify(pizzaRepository).save(testPizza);
        verify(pizzaMapper).toResponse(testPizza);
    }

    @Test
    void update_ExistingPizza_ReturnsUpdatedPizza() {
        // Given
        UpdatePizzaRequest updateRequest = new UpdatePizzaRequest(
                "Margherita Special",
                new BigDecimal("9.50"),
                "Updated description",
                true,
                null
        );

        when(pizzaRepository.findById("1")).thenReturn(Optional.of(testPizza));
        when(pizzaRepository.save(testPizza)).thenReturn(testPizza);
        when(pizzaMapper.toResponse(testPizza)).thenReturn(testResponse);

        // When
        PizzaResponse result = pizzaService.update("1", updateRequest);

        // Then
        assertThat(result).isNotNull();

        verify(pizzaRepository).findById("1");
        verify(pizzaMapper).updateEntity(updateRequest, testPizza);
        verify(pizzaRepository).save(testPizza);
        verify(pizzaMapper).toResponse(testPizza);
    }

    @Test
    void update_NonExistingPizza_ThrowsResourceNotFoundException() {
        // Given
        UpdatePizzaRequest updateRequest = new UpdatePizzaRequest(
                "Updated Name",
                new BigDecimal("10.00"),
                "Updated description",
                true,
                null
        );
        when(pizzaRepository.findById("999")).thenReturn(Optional.empty());

        // When
        // When & Then
        assertThatThrownBy(() -> pizzaService.update("999", updateRequest))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(pizzaRepository).findById("999");
        verify(pizzaRepository, never()).save(any());
    }

    @Test
    void delete_ExistingPizza_DeletesPizza() {
        // Given
        when(pizzaRepository.existsById("1")).thenReturn(true);

        // When
        pizzaService.delete("1");

        // Then
        verify(pizzaRepository).existsById("1");
        verify(pizzaRepository).deleteById("1");
    }

    @Test
    void delete_NonExistingPizza_ThrowsResourceNotFoundException() {
        // Given
        when(pizzaRepository.existsById("999")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> pizzaService.delete("999"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(pizzaRepository).existsById("999");
        verify(pizzaRepository, never()).deleteById(any());
    }

    @Test
    void delete_PizzaInAnOrder_ThrowsDataIntegrityViolation() {
        // Given: MongoDB has no foreign key to refuse this, so the service checks it (409 Conflict, like with JPA)
        when(pizzaRepository.existsById("1")).thenReturn(true);
        when(orderRepository.existsByOrderLinesPizzaId("1")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> pizzaService.delete("1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(pizzaRepository, never()).deleteById(any());
    }

    @Test
    void delete_FavoritePizza_ThrowsDataIntegrityViolation() {
        // Given
        when(pizzaRepository.existsById("1")).thenReturn(true);
        when(orderRepository.existsByOrderLinesPizzaId("1")).thenReturn(false);
        when(customerRepository.existsByFavoritePizzaIds("1")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> pizzaService.delete("1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(pizzaRepository, never()).deleteById(any());
    }

    @Test
    void findAll_WithPageable_ReturnsPageOfPizzas() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Pizza> pizzas = Arrays.asList(testPizza);
        Page<Pizza> pizzaPage = new PageImpl<>(pizzas, pageable, 1);
        
        when(pizzaRepository.findAll(pageable)).thenReturn(pizzaPage);
        when(pizzaMapper.toResponse(testPizza)).thenReturn(testResponse);

        // When
        Page<PizzaResponse> result = pizzaService.findAll(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Margherita");
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(pizzaRepository).findAll(pageable);
        verify(pizzaMapper).toResponse(testPizza);
    }

    @Test
    void findAll_WithPageable_EmptyPage_ReturnsEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pizza> emptyPage = Page.empty();
        
        when(pizzaRepository.findAll(pageable)).thenReturn(emptyPage);

        // When
        Page<PizzaResponse> result = pizzaService.findAll(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(pizzaRepository).findAll(pageable);
        verify(pizzaMapper, never()).toResponse(any());
    }

    @Test
    void findByPriceBetween_MultipleResults_ReturnsListOfResponses() {
        // Given
        Pizza pizza2 = new Pizza("Marinara", new BigDecimal("7.50"), "Simple");
        pizza2.setId("2");

        List<Pizza> pizzas = Arrays.asList(testPizza, pizza2);
        List<PizzaResponse> responses = Arrays.asList(
                testResponse,
                new PizzaResponse("2", "Marinara", new BigDecimal("7.50"), "Simple", null, true, null)
        );

        when(pizzaRepository.findByPriceBetween(new BigDecimal("7.00"), new BigDecimal("9.00")))
                .thenReturn(pizzas);
        when(pizzaMapper.toResponseList(pizzas)).thenReturn(responses);

        // When
        List<PizzaResponse> result = pizzaService.findByPriceBetween(new BigDecimal("7.00"), new BigDecimal("9.00"));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PizzaResponse::name)
                .containsExactly("Margherita", "Marinara");

        verify(pizzaRepository).findByPriceBetween(new BigDecimal("7.00"), new BigDecimal("9.00"));
        verify(pizzaMapper).toResponseList(pizzas);
    }

    @Test
    void findByPriceBetween_NoResults_ReturnsEmptyList() {
        // Given
        when(pizzaRepository.findByPriceBetween(new BigDecimal("100.00"), new BigDecimal("200.00")))
                .thenReturn(List.of());
        when(pizzaMapper.toResponseList(List.of())).thenReturn(List.of());

        // When
        List<PizzaResponse> result = pizzaService.findByPriceBetween(new BigDecimal("100.00"), new BigDecimal("200.00"));

        // Then
        assertThat(result).isEmpty();

        verify(pizzaRepository).findByPriceBetween(new BigDecimal("100.00"), new BigDecimal("200.00"));
    }

    @Test
    void findByNameContaining_MultipleResults_ReturnsListOfResponses() {
        // Given
        Pizza pizza2 = new Pizza("Marinara", new BigDecimal("7.50"), "Simple");
        pizza2.setId("2");

        List<Pizza> pizzas = Arrays.asList(testPizza, pizza2);
        List<PizzaResponse> responses = Arrays.asList(
                testResponse,
                new PizzaResponse("2", "Marinara", new BigDecimal("7.50"), "Simple", null, true, null)
        );

        when(pizzaRepository.findByNameContainingIgnoreCase("mar")).thenReturn(pizzas);
        when(pizzaMapper.toResponseList(pizzas)).thenReturn(responses);

        // When
        List<PizzaResponse> result = pizzaService.findByNameContaining("mar");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PizzaResponse::name)
                .containsExactly("Margherita", "Marinara");

        verify(pizzaRepository).findByNameContainingIgnoreCase("mar");
        verify(pizzaMapper).toResponseList(pizzas);
    }

    @Test
    void findByNameContaining_NoResults_ReturnsEmptyList() {
        // Given
        when(pizzaRepository.findByNameContainingIgnoreCase("xyz")).thenReturn(List.of());
        when(pizzaMapper.toResponseList(List.of())).thenReturn(List.of());

        // When
        List<PizzaResponse> result = pizzaService.findByNameContaining("xyz");

        // Then
        assertThat(result).isEmpty();

        verify(pizzaRepository).findByNameContainingIgnoreCase("xyz");
    }

    @Test
    void uploadImage_ExistingPizza_ReturnsUpdatedPizza() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        String imageUrl = "/uploads/pizza-1.jpg";
        
        PizzaResponse updatedResponse = new PizzaResponse("1", "Margherita", new BigDecimal("8.50"), 
                "Classic tomato and mozzarella", imageUrl, true, null);

        when(pizzaRepository.findById("1")).thenReturn(Optional.of(testPizza));
        when(fileStorageService.storeFile(mockFile, "1")).thenReturn(imageUrl);
        when(pizzaRepository.save(testPizza)).thenReturn(testPizza);
        when(pizzaMapper.toResponse(testPizza)).thenReturn(updatedResponse);

        // When
        PizzaResponse result = pizzaService.uploadImage("1", mockFile);

        // Then
        assertThat(result.imageUrl()).isEqualTo(imageUrl);

        verify(pizzaRepository).findById("1");
        verify(fileStorageService).storeFile(mockFile, "1");
        verify(pizzaRepository).save(testPizza);
        verify(pizzaMapper).toResponse(testPizza);
    }

    @Test
    void uploadImage_NonExistingPizza_ThrowsResourceNotFoundException() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(pizzaRepository.findById("999")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> pizzaService.uploadImage("999", mockFile))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(pizzaRepository).findById("999");
        verify(fileStorageService, never()).storeFile(any(), any());
        verify(pizzaRepository, never()).save(any());
    }

    @Test
    void create_WithNutritionalInfo_SavesItWithThePizza() {
        // Given
        CreatePizzaRequest request = new CreatePizzaRequest(
                "Margherita", 
                new BigDecimal("8.50"), 
                "Classic tomato and mozzarella", 
                true, 
                null
        );
        
        Pizza pizzaWithNutritionalInfo = new Pizza("Margherita", new BigDecimal("8.50"), "Classic");
        be.vives.pizzastore.domain.NutritionalInfo nutritionalInfo = new be.vives.pizzastore.domain.NutritionalInfo();
        pizzaWithNutritionalInfo.setNutritionalInfo(nutritionalInfo);
        
        when(pizzaMapper.toEntity(request)).thenReturn(pizzaWithNutritionalInfo);
        when(pizzaRepository.save(pizzaWithNutritionalInfo)).thenReturn(pizzaWithNutritionalInfo);
        when(pizzaMapper.toResponse(pizzaWithNutritionalInfo)).thenReturn(testResponse);

        // When
        PizzaResponse result = pizzaService.create(request);

        // Then: embedded in the pizza document, so saving the pizza saves it too (no bidirectional link to set)
        assertThat(result).isNotNull();
        assertThat(pizzaWithNutritionalInfo.getNutritionalInfo()).isSameAs(nutritionalInfo);

        verify(pizzaMapper).toEntity(request);
        verify(pizzaRepository).save(pizzaWithNutritionalInfo);
        verify(pizzaMapper).toResponse(pizzaWithNutritionalInfo);
    }

    @Test
    void update_WithNutritionalInfo_SavesItWithThePizza() {
        // Given
        UpdatePizzaRequest updateRequest = new UpdatePizzaRequest(
                "Margherita Special",
                new BigDecimal("9.50"),
                "Updated description",
                true,
                null
        );

        Pizza pizzaWithNutritionalInfo = new Pizza("Margherita", new BigDecimal("8.50"), "Classic");
        pizzaWithNutritionalInfo.setId("1");
        be.vives.pizzastore.domain.NutritionalInfo nutritionalInfo = new be.vives.pizzastore.domain.NutritionalInfo();
        pizzaWithNutritionalInfo.setNutritionalInfo(nutritionalInfo);

        when(pizzaRepository.findById("1")).thenReturn(Optional.of(pizzaWithNutritionalInfo));
        when(pizzaRepository.save(pizzaWithNutritionalInfo)).thenReturn(pizzaWithNutritionalInfo);
        when(pizzaMapper.toResponse(pizzaWithNutritionalInfo)).thenReturn(testResponse);

        // When
        PizzaResponse result = pizzaService.update("1", updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(pizzaWithNutritionalInfo.getNutritionalInfo()).isSameAs(nutritionalInfo);

        verify(pizzaRepository).findById("1");
        verify(pizzaMapper).updateEntity(updateRequest, pizzaWithNutritionalInfo);
        verify(pizzaRepository).save(pizzaWithNutritionalInfo);
    }

    @Test
    void updateNutritionalInfo_PizzaWithoutInfo_CreatesIt() {
        NutritionalInfoRequest request = new NutritionalInfoRequest(539, new BigDecimal("6.30"), new BigDecimal("57.50"), new BigDecimal("30.90"));
        when(pizzaRepository.findById("1")).thenReturn(Optional.of(testPizza));
        when(pizzaRepository.save(testPizza)).thenReturn(testPizza);
        when(pizzaMapper.toResponse(testPizza)).thenReturn(testResponse);

        pizzaService.updateNutritionalInfo("1", request);

        NutritionalInfo info = testPizza.getNutritionalInfo();
        assertThat(info).isNotNull();
        assertThat(info.getCalories()).isEqualTo(539);
        assertThat(info.getProtein()).isEqualByComparingTo("6.30");
        assertThat(info.getCarbohydrates()).isEqualByComparingTo("57.50");
        assertThat(info.getFat()).isEqualByComparingTo("30.90");
    }

    @Test
    void updateNutritionalInfo_PizzaWithInfo_OverwritesTheExistingInfo() {
        NutritionalInfo existing = new NutritionalInfo(100, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        testPizza.setNutritionalInfo(existing);
        NutritionalInfoRequest request = new NutritionalInfoRequest(539, new BigDecimal("6.30"), new BigDecimal("57.50"), new BigDecimal("30.90"));
        when(pizzaRepository.findById("1")).thenReturn(Optional.of(testPizza));
        when(pizzaRepository.save(testPizza)).thenReturn(testPizza);
        when(pizzaMapper.toResponse(testPizza)).thenReturn(testResponse);

        pizzaService.updateNutritionalInfo("1", request);

        assertThat(testPizza.getNutritionalInfo()).isSameAs(existing);   // updated in place
        assertThat(existing.getCalories()).isEqualTo(539);
    }

    @Test
    void updateNutritionalInfo_NonExistingPizza_ThrowsResourceNotFoundException() {
        when(pizzaRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pizzaService.updateNutritionalInfo("999", new NutritionalInfoRequest(1, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(pizzaRepository, never()).save(any());
    }
}
