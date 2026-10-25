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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
// No @Transactional (the JPA version has it on the class): Spring Boot configures no transaction manager for
// MongoDB, and multi-document transactions need a replica set, not the single MongoDB server used here.
// Each save() of one document is atomic on its own, and a document holds a whole aggregate (a pizza with its
// nutritional info), so most operations touch only one document anyway.
public class PizzaService {

    private static final Logger log = LoggerFactory.getLogger(PizzaService.class);

    private final PizzaRepository pizzaRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PizzaMapper pizzaMapper;
    private final FileStorageService fileStorageService;

    public PizzaService(PizzaRepository pizzaRepository,
                        OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        PizzaMapper pizzaMapper,
                        FileStorageService fileStorageService) {
        this.pizzaRepository = pizzaRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.pizzaMapper = pizzaMapper;
        this.fileStorageService = fileStorageService;
    }

    public Page<PizzaResponse> findAll(Pageable pageable) {
        log.debug("Finding pizzas with pagination: {}", pageable);
        Page<Pizza> pizzaPage = pizzaRepository.findAll(pageable);
        return pizzaPage.map(pizzaMapper::toResponse);
    }

    public PizzaResponse findById(String id) {
        log.debug("Finding pizza with id: {}", id);
        Pizza pizza = pizzaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pizza", id));
        return pizzaMapper.toResponse(pizza);
    }

    public List<PizzaResponse> findByPriceLessThan(BigDecimal maxPrice) {
        log.debug("Finding pizzas with price less than: {}", maxPrice);
        List<Pizza> pizzas = pizzaRepository.findByPriceLessThan(maxPrice);
        return pizzaMapper.toResponseList(pizzas);
    }

    public List<PizzaResponse> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Finding pizzas with price between {} and {}", minPrice, maxPrice);
        List<Pizza> pizzas = pizzaRepository.findByPriceBetween(minPrice, maxPrice);
        return pizzaMapper.toResponseList(pizzas);
    }

    public List<PizzaResponse> findByNameContaining(String name) {
        log.debug("Finding pizzas with name containing: {}", name);
        List<Pizza> pizzas = pizzaRepository.findByNameContainingIgnoreCase(name);
        return pizzaMapper.toResponseList(pizzas);
    }

    public PizzaResponse create(CreatePizzaRequest request) {
        log.debug("Creating new pizza: {}", request.name());
        // the nutritional info is embedded in the pizza document: no bidirectional relationship to set
        Pizza pizza = pizzaMapper.toEntity(request);
        Pizza savedPizza = pizzaRepository.save(pizza);
        log.info("Created pizza with id: {}", savedPizza.getId());
        return pizzaMapper.toResponse(savedPizza);
    }

    public PizzaResponse update(String id, UpdatePizzaRequest request) {
        log.debug("Updating pizza with id: {}", id);
        Pizza pizza = pizzaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pizza", id));

        pizzaMapper.updateEntity(request, pizza);
        Pizza updatedPizza = pizzaRepository.save(pizza);
        log.info("Updated pizza with id: {}", id);
        return pizzaMapper.toResponse(updatedPizza);
    }

    /** Creates or overwrites the nutritional info of a pizza (used by the Open Food Facts import). */
    public PizzaResponse updateNutritionalInfo(String id, NutritionalInfoRequest request) {
        log.debug("Updating nutritional info of pizza with id: {}", id);
        Pizza pizza = pizzaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pizza", id));

        NutritionalInfo info = pizza.getNutritionalInfo();
        if (info == null) {
            info = new NutritionalInfo();
            pizza.setNutritionalInfo(info);
        }
        info.setCalories(request.calories());
        info.setProtein(request.protein());
        info.setCarbohydrates(request.carbohydrates());
        info.setFat(request.fat());

        return pizzaMapper.toResponse(pizzaRepository.save(pizza));
    }

    public void delete(String id) {
        log.debug("Deleting pizza with id: {}", id);
        if (!pizzaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pizza", id);
        }
        // With JPA the foreign keys of order_lines and customer_favorite_pizzas refuse to delete a pizza that is
        // still used, and that DataIntegrityViolationException becomes a 409 Conflict. MongoDB has no foreign keys:
        // without this check the orders and favorites would silently keep the id of a pizza that no longer exists.
        if (orderRepository.existsByOrderLinesPizzaId(id) || customerRepository.existsByFavoritePizzaIds(id)) {
            throw new DataIntegrityViolationException("Pizza " + id + " is still referenced by orders or favorites");
        }
        pizzaRepository.deleteById(id);
        log.info("Deleted pizza with id: {}", id);
    }

    public PizzaResponse uploadImage(String id, MultipartFile file) {
        log.debug("Uploading image for pizza with id: {}", id);
        Pizza pizza = pizzaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pizza", id));

        String imageUrl = fileStorageService.storeFile(file, id);
        pizza.setImageUrl(imageUrl);
        Pizza updatedPizza = pizzaRepository.save(pizza);
        log.info("Updated image URL for pizza with id: {}", id);
        return pizzaMapper.toResponse(updatedPizza);
    }
}
