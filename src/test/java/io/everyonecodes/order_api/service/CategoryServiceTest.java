package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.repository.CategoryRepository;
import io.everyonecodes.order_api.repository.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @InjectMocks
    private CategoryService service;

    @Mock
    private CategoryRepository repository;
    @Mock
    private MenuItemRepository menuItemRepository;

    @Test
    void findAll() {

        List<Category> allCategories = List.of(
                new Category(1L, "Pizzas", true, new HashSet<>()),
                new Category(2L, "Deserts", false, new HashSet<>()),
                new Category(3L, "Drinks", true, new HashSet<>())
        );

        when(repository.findAll()).thenReturn(allCategories);

        var result = service.findAll();

        var expected = List.of(
                new Category(1L, "Pizzas", true, new HashSet<>()),
                new Category(2L, "Deserts", false, new HashSet<>()),
                new Category(3L, "Drinks", true, new HashSet<>())
        );

        assertEquals(expected, result);

        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findAllActive() {
        var expected = List.of(
                new Category(1L, "Pizzas", true, new HashSet<>()),
                new Category(3L, "Drinks", true, new HashSet<>())
                );

        when(repository.findAllByIsActive(true)).thenReturn(expected);

        var result = service.findAllActive();

        assertEquals(expected, result);

        verify(repository).findAllByIsActive(true);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getByIdAndIsActiveOrThrow_withExistingCategory() {
        Long id = 1L;
        var expected = new Category(id, "Pizzas", true, new HashSet<>());

        when(repository.findByIdAndIsActiveTrue(id)).thenReturn(Optional.of(expected));

        var result = service.getByIdAndIsActiveOrThrow(id);

        assertEquals(expected, result);

        verify(repository).findByIdAndIsActiveTrue(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getByIdAndIsActiveOrThrow_withNonExistingCategory_throws() {
        Long id = 4L;

        when(repository.findByIdAndIsActiveTrue(id)).thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class, () -> service.getByIdAndIsActiveOrThrow(id));

        String expectedMessage = "Category " + id + " not found";
        assertEquals(expectedMessage, exception.getMessage());

        verify(repository).findByIdAndIsActiveTrue(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void createCategory() {
        var inputCategory = new Category(5L, "Salads", null, new HashSet<>());
        var expectedSaved = new Category(null, "Salads", true, new HashSet<>());

        when(repository.save(any(Category.class))).thenReturn(expectedSaved);

        var result = service.createCategory(inputCategory);

        assertEquals(expectedSaved, result);
        assertTrue(expectedSaved.getIsActive());
        verify(repository).save(any(Category.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void createCategory_preservesAnExplicitActiveFlag() {
        var inputCategory = new Category(5L, "Archived Salads", false, new HashSet<>());

        when(repository.save(inputCategory)).thenReturn(inputCategory);

        var result = service.createCategory(inputCategory);

        assertNull(result.getId());
        assertFalse(result.getIsActive());
        verify(repository).save(inputCategory);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void softDeleteCategoryById() {
        Long id = 1L;
        var existingCategory = new Category(id, "Pizzas", true, new HashSet<>());

        when(repository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(repository.save(any(Category.class))).thenReturn(existingCategory);

        service.softDeleteCategoryById(id);

        assertFalse(existingCategory.getIsActive());
        verify(repository).findById(id);
        verify(repository).save(existingCategory);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void updateCategory() {
        Long id = 1L;

        var inputCategory = new Category();
        inputCategory.setName("Updated Pizza");
        inputCategory.setIsActive(false);

        var existingCategory = new Category(id, "Old Pizza", true, new HashSet<>());

        when(repository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(repository.save(existingCategory)).thenReturn(existingCategory);

        var result = service.updateCategory(inputCategory, id);

        assertEquals("Updated Pizza", result.getName());
        assertFalse(result.getIsActive());

        verify(repository).findById(id);
        verify(repository).save(existingCategory);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void softDeleteCategoryById_switchesOffTheActiveItemsAndRemembersThem() {
        Long id = 1L;
        var existingCategory = new Category(id, "Pizzas", true, new HashSet<>());
        var activeItem = new MenuItem();
        activeItem.setIsActive(true);
        var items = List.of(activeItem);

        when(repository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(menuItemRepository.findByCategoryAndIsActive(existingCategory, true)).thenReturn(items);

        service.softDeleteCategoryById(id);

        assertFalse(activeItem.getIsActive());
        assertTrue(activeItem.getDeactivatedWithCategory());
        verify(menuItemRepository).saveAll(items);
    }

    @Test
    void updateCategory_switchesOffTheActiveItems_whenTheCategoryIsTurnedOff() {
        Long id = 1L;
        var inputCategory = new Category();
        inputCategory.setName("Pizzas");
        inputCategory.setIsActive(false);
        var existingCategory = new Category(id, "Pizzas", true, new HashSet<>());
        var activeItem = new MenuItem();
        activeItem.setIsActive(true);
        var items = List.of(activeItem);

        when(repository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(repository.save(existingCategory)).thenReturn(existingCategory);
        when(menuItemRepository.findByCategoryAndIsActive(existingCategory, true)).thenReturn(items);

        service.updateCategory(inputCategory, id);

        assertFalse(activeItem.getIsActive());
        assertTrue(activeItem.getDeactivatedWithCategory());
        verify(menuItemRepository).saveAll(items);
    }

    @Test
    void updateCategory_leavesTheItemsAlone_whenTheCategoryStaysActive() {
        Long id = 1L;
        var inputCategory = new Category();
        inputCategory.setName("Renamed Pizzas");
        inputCategory.setIsActive(true);
        var existingCategory = new Category(id, "Pizzas", true, new HashSet<>());

        when(repository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(repository.save(existingCategory)).thenReturn(existingCategory);

        service.updateCategory(inputCategory, id);

        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void findByIdOrThrow_withExistingCategory() {
        var category = new Category(1L, "Pizzas", true, new HashSet<>());
        when(repository.findById(1L)).thenReturn(Optional.of(category));

        assertEquals(category, service.findByIdOrThrow(1L));

        verify(repository).findById(1L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findByIdOrThrow_withMissingCategory_throws() {
        when(repository.findById(4L)).thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class, () -> service.findByIdOrThrow(4L));

        assertEquals("Category 4 not found", exception.getMessage());
        verify(repository).findById(4L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findItemsOfCategory_returnsEveryItemWithItsRememberFlag() {
        Long id = 1L;
        var category = new Category(id, "Pizzas", false, new HashSet<>());

        var switchedOffByCategory = new MenuItem();
        switchedOffByCategory.setId(10L);
        switchedOffByCategory.setName("Margherita");
        switchedOffByCategory.setPrice(new BigDecimal("8.50"));
        switchedOffByCategory.setIsActive(false);
        switchedOffByCategory.setDeactivatedWithCategory(true);

        var offBeforeAlready = new MenuItem();
        offBeforeAlready.setId(11L);
        offBeforeAlready.setName("Old Special");
        offBeforeAlready.setPrice(new BigDecimal("9.00"));
        offBeforeAlready.setIsActive(false);

        when(repository.findById(id)).thenReturn(Optional.of(category));
        when(menuItemRepository.findByCategoryOrderByIdAsc(category)).thenReturn(List.of(switchedOffByCategory, offBeforeAlready));

        var result = service.findItemsOfCategory(id);

        assertEquals(2, result.size());
        assertEquals("Margherita", result.getFirst().getName());
        assertFalse(result.get(0).getIsActive());
        assertTrue(result.get(0).getWasActiveBeforeTurnOff());
        assertEquals("Old Special", result.get(1).getName());
        assertFalse(result.get(1).getIsActive());
        assertFalse(result.get(1).getWasActiveBeforeTurnOff());
    }

    @Test
    void findItemsOfCategory_withMissingCategory_throws() {
        when(repository.findById(4L)).thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class, () -> service.findItemsOfCategory(4L));

        assertEquals("Category 4 not found", exception.getMessage());
        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void reactivateCategory_switchesOnTheCategoryAndThePickedItems_andClearsAllFlags() {
        Long id = 1L;
        var category = new Category(id, "Pizzas", false, new HashSet<>());
        var picked = new MenuItem();
        picked.setId(10L);
        picked.setIsActive(false);
        picked.setDeactivatedWithCategory(true);
        var notPicked = new MenuItem();
        notPicked.setId(11L);
        notPicked.setIsActive(false);
        notPicked.setDeactivatedWithCategory(true);
        var items = List.of(picked, notPicked);

        when(repository.findById(id)).thenReturn(Optional.of(category));
        when(menuItemRepository.findByCategoryOrderByIdAsc(category)).thenReturn(items);
        when(repository.save(category)).thenReturn(category);

        var result = service.reactivateCategory(id, Set.of(10L));

        assertTrue(result.getIsActive());
        assertTrue(picked.getIsActive());
        assertFalse(picked.getDeactivatedWithCategory());
        assertFalse(notPicked.getIsActive());
        assertFalse(notPicked.getDeactivatedWithCategory());
        verify(menuItemRepository).saveAll(items);
    }

    @Test
    void reactivateCategory_withNoPickedItems_switchesOnOnlyTheCategory() {
        Long id = 1L;
        var category = new Category(id, "Pizzas", false, new HashSet<>());
        var item = new MenuItem();
        item.setId(10L);
        item.setIsActive(false);
        item.setDeactivatedWithCategory(true);

        when(repository.findById(id)).thenReturn(Optional.of(category));
        when(menuItemRepository.findByCategoryOrderByIdAsc(category)).thenReturn(List.of(item));
        when(repository.save(category)).thenReturn(category);

        var result = service.reactivateCategory(id, null);

        assertTrue(result.getIsActive());
        assertFalse(item.getIsActive());
        assertFalse(item.getDeactivatedWithCategory());
    }

    @Test
    void reactivateCategory_withAnItemFromAnotherCategory_throwsAndChangesNothing() {
        Long id = 1L;
        var category = new Category(id, "Pizzas", false, new HashSet<>());
        var item = new MenuItem();
        item.setId(10L);
        item.setIsActive(false);

        when(repository.findById(id)).thenReturn(Optional.of(category));
        when(menuItemRepository.findByCategoryOrderByIdAsc(category)).thenReturn(List.of(item));

        var exception = assertThrows(ResourceNotFoundException.class,
                () -> service.reactivateCategory(id, Set.of(10L, 99L)));

        assertEquals("One or more menu items were not found in category 1", exception.getMessage());
        assertFalse(category.getIsActive());
        verify(menuItemRepository, never()).saveAll(any());
        verify(repository, never()).save(any(Category.class));
    }

    @Test
    void reactivateCategory_withMissingCategory_throws() {
        when(repository.findById(4L)).thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class,
                () -> service.reactivateCategory(4L, Set.of()));

        assertEquals("Category 4 not found", exception.getMessage());
        verifyNoInteractions(menuItemRepository);
    }
}