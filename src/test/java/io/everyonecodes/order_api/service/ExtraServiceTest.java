package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.dto.ExtraRequestDto;
import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.repository.ExtraRepository;
import io.everyonecodes.order_api.repository.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtraServiceTest {

    @InjectMocks
    private ExtraService service;

    @Mock
    private ExtraRepository repository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Test
    void findAll() {
        List<Extra> extras = List.of(
                new Extra(1L, "Extra Cheese", BigDecimal.valueOf(1.5), true, new HashSet<>()),
                new Extra(2L, "Bacon", BigDecimal.valueOf(2), false, new HashSet<>()),
                new Extra(3L, "Mushrooms", BigDecimal.valueOf(1), true, new HashSet<>())
        );

        when(repository.findAll()).thenReturn(extras);

        var result = service.findAll();

        List<Extra> expected = List.of(
                new Extra(1L, "Extra Cheese", BigDecimal.valueOf(1.5), true, new HashSet<>()),
                new Extra(2L, "Bacon", BigDecimal.valueOf(2), false, new HashSet<>()),
                new Extra(3L, "Mushrooms", BigDecimal.valueOf(1), true, new HashSet<>())
        );

        assertEquals(expected, result);

        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findByMenuItemsContainingAndIsActive() {
        var item = new MenuItem();
        item.setId(1L);
        item.setName("Burger");
        item.setIsActive(true);

        var extra = new Extra();
        extra.setId(10L);
        extra.setName("Extra Cheese");
        extra.setIsActive(true);

        when(repository.findByMenuItemsContainingAndIsActive(item, true)).thenReturn(List.of(extra));

        var result = service.findByMenuItemsContainingAndIsActive(item);

        assertEquals(extra, result.getFirst());

        verify(repository).findByMenuItemsContainingAndIsActive(item, true);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findAllById_passingCase() {
        Set<Long> ids = Set.of(1L, 3L);

        var expected = List.of(
                new Extra(1L, "Extra Cheese", BigDecimal.valueOf(1.5), true, new HashSet<>()),
                new Extra(3L, "Mushrooms", BigDecimal.valueOf(1), true, new HashSet<>())
        );

        when(repository.findAllById(ids)).thenReturn(expected);

        var result = service.findAllById(ids);

        assertEquals(expected, result);

        verify(repository).findAllById(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findAllById_returnsFewerExtras_whenSomeIdsDontExist() {
        Set<Long> ids = Set.of(1L, 2L, 3L);

        var founded = List.of(
                new Extra(1L, "Extra Cheese", BigDecimal.valueOf(1.5), true, new HashSet<>()),
                new Extra(3L, "Mushrooms", BigDecimal.valueOf(1), true, new HashSet<>())
        );

        when(repository.findAllById(ids)).thenReturn(founded);

        var result = service.findAllById(ids);

        assertEquals(2, result.size());
        assertNotEquals(ids.size(), result.size());

        verify(repository).findAllById(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void createExtra_defaultsToActive() {
        var dto = new ExtraRequestDto("Olives", BigDecimal.ONE, null, null);
        when(repository.save(any(Extra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createExtra(dto);

        assertNull(result.getId());
        assertEquals("Olives", result.getName());
        assertEquals(BigDecimal.ONE, result.getPrice());
        assertTrue(result.getIsActive());
        verify(repository).save(any(Extra.class));
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void createExtra_preservesAnExplicitInactiveFlag() {
        var dto = new ExtraRequestDto("Archived Olives", BigDecimal.ONE, false, null);
        when(repository.save(any(Extra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createExtra(dto);

        assertFalse(result.getIsActive());
        verify(repository).save(any(Extra.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void createExtra_preservesAnExplicitActiveFlag() {
        var dto = new ExtraRequestDto("Olives", BigDecimal.ONE, true, null);
        when(repository.save(any(Extra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createExtra(dto);

        assertTrue(result.getIsActive());
    }

    @Test
    void createExtra_linksTheRequestedMenuItems() {
        var burger = new MenuItem(1L, "Burger", "", BigDecimal.TEN, "", true, new HashSet<>(), null);
        var wrap = new MenuItem(2L, "Wrap", "", BigDecimal.TEN, "", true, new HashSet<>(), null);
        var dto = new ExtraRequestDto("Bacon", BigDecimal.TWO, true, Set.of(1L, 2L));
        when(menuItemRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(burger, wrap));
        when(repository.save(any(Extra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createExtra(dto);

        assertEquals(2, result.getMenuItems().size());
        assertTrue(result.getMenuItems().contains(burger));
        assertTrue(result.getMenuItems().contains(wrap));
        verify(menuItemRepository).findAllById(Set.of(1L, 2L));
        verify(repository).save(any(Extra.class));
    }

    @Test
    void createExtra_throwsWhenAMenuItemDoesNotExist() {
        var burger = new MenuItem(1L, "Burger", "", BigDecimal.TEN, "", true, new HashSet<>(), null);
        var dto = new ExtraRequestDto("Bacon", BigDecimal.TWO, true, Set.of(1L, 999L));
        when(menuItemRepository.findAllById(Set.of(1L, 999L))).thenReturn(List.of(burger));

        var exception = assertThrows(ResourceNotFoundException.class, () -> service.createExtra(dto));

        assertEquals("One or more menu items were not found", exception.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void updateExtra_updatesEveryMutableField() {
        var existing = new Extra(1L, "Cheese", BigDecimal.ONE, true, new HashSet<>());
        var dto = new ExtraRequestDto("Bacon", BigDecimal.valueOf(2), false, null);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var result = service.updateExtra(dto, 1L);

        assertEquals("Bacon", result.getName());
        assertEquals(BigDecimal.valueOf(2), result.getPrice());
        assertFalse(result.getIsActive());
        verify(repository).findById(1L);
        verify(repository).save(existing);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void updateExtra_replacesTheLinkedMenuItems() {
        var burger = new MenuItem(1L, "Burger", "", BigDecimal.TEN, "", true, new HashSet<>(), null);
        var wrap = new MenuItem(2L, "Wrap", "", BigDecimal.TEN, "", true, new HashSet<>(), null);
        var existing = new Extra(1L, "Cheese", BigDecimal.ONE, true, new HashSet<>(Set.of(burger)));
        var dto = new ExtraRequestDto("Cheese", BigDecimal.ONE, true, Set.of(2L));
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(menuItemRepository.findAllById(Set.of(2L))).thenReturn(List.of(wrap));
        when(repository.save(existing)).thenReturn(existing);

        var result = service.updateExtra(dto, 1L);

        assertEquals(1, result.getMenuItems().size());
        assertTrue(result.getMenuItems().contains(wrap));
        assertFalse(result.getMenuItems().contains(burger));
    }

    @Test
    void updateExtra_withEmptyMenuItemIds_removesAllLinks() {
        var burger = new MenuItem(1L, "Burger", "", BigDecimal.TEN, "", true, new HashSet<>(), null);
        var existing = new Extra(1L, "Cheese", BigDecimal.ONE, true, new HashSet<>(Set.of(burger)));
        var dto = new ExtraRequestDto("Cheese", BigDecimal.ONE, true, Set.of());
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(menuItemRepository.findAllById(Set.of())).thenReturn(List.of());
        when(repository.save(existing)).thenReturn(existing);

        var result = service.updateExtra(dto, 1L);

        assertTrue(result.getMenuItems().isEmpty());
    }

    @Test
    void updateExtra_withoutMenuItemIds_leavesTheLinksUnchanged() {
        var burger = new MenuItem(1L, "Burger", "", BigDecimal.TEN, "", true, new HashSet<>(), null);
        var existing = new Extra(1L, "Cheese", BigDecimal.ONE, true, new HashSet<>(Set.of(burger)));
        var dto = new ExtraRequestDto("Cheese", BigDecimal.ONE, true, null);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var result = service.updateExtra(dto, 1L);

        assertEquals(1, result.getMenuItems().size());
        assertTrue(result.getMenuItems().contains(burger));
        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void updateExtra_throwsWhenAMenuItemDoesNotExist() {
        var existing = new Extra(1L, "Cheese", BigDecimal.ONE, true, new HashSet<>());
        var dto = new ExtraRequestDto("Cheese", BigDecimal.ONE, true, Set.of(999L));
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(menuItemRepository.findAllById(Set.of(999L))).thenReturn(List.of());

        var exception = assertThrows(ResourceNotFoundException.class, () -> service.updateExtra(dto, 1L));

        assertEquals("One or more menu items were not found", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void softDeleteById_marksExtraInactive() {
        var extra = new Extra(1L, "Cheese", BigDecimal.ONE, true, new HashSet<>());
        when(repository.findById(1L)).thenReturn(Optional.of(extra));

        service.softDeleteById(1L);

        assertFalse(extra.getIsActive());
        verify(repository).findById(1L);
        verify(repository).save(extra);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findExtraByIdOrThrow_returnsExtraOrThrows() {
        var extra = new Extra(1L, "Cheese", BigDecimal.ONE, true, new HashSet<>());
        when(repository.findById(1L)).thenReturn(Optional.of(extra));
        when(repository.findById(2L)).thenReturn(Optional.empty());

        assertEquals(extra, service.findExtraByIdOrThrow(1L));
        var exception = assertThrows(ResourceNotFoundException.class, () -> service.findExtraByIdOrThrow(2L));
        assertEquals("Extra 2 not found", exception.getMessage());

        verify(repository).findById(1L);
        verify(repository).findById(2L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void addMenuItem_addsTheMenuItemToTheExtraAndSaves() {
        var menuItem = new MenuItem();
        menuItem.setId(1L);
        var extra = new Extra(10L, "Cheese", BigDecimal.ONE, true, new HashSet<>());
        when(repository.findById(10L)).thenReturn(Optional.of(extra));

        service.addMenuItem(menuItem, 10L);

        assertEquals(1, extra.getMenuItems().size());
        assertTrue(extra.getMenuItems().contains(menuItem));
        verify(repository).findById(10L);
        verify(repository).save(extra);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void addMenuItem_twice_keepsASingleEntry() {
        var menuItem = new MenuItem();
        menuItem.setId(1L);
        var extra = new Extra(10L, "Cheese", BigDecimal.ONE, true, new HashSet<>());
        when(repository.findById(10L)).thenReturn(Optional.of(extra));

        service.addMenuItem(menuItem, 10L);
        service.addMenuItem(menuItem, 10L);

        assertEquals(1, extra.getMenuItems().size());
        verify(repository, times(2)).findById(10L);
        verify(repository, times(2)).save(extra);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void addMenuItem_throwsWhenTheExtraDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class, () -> service.addMenuItem(new MenuItem(), 99L));

        assertEquals("Extra 99 not found", exception.getMessage());
        verify(repository).findById(99L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void removeMenuItem_removesTheMenuItemFromTheExtraAndSaves() {
        var menuItem = new MenuItem();
        menuItem.setId(1L);
        var extra = new Extra(10L, "Cheese", BigDecimal.ONE, true, new HashSet<>());
        extra.getMenuItems().add(menuItem);
        when(repository.findById(10L)).thenReturn(Optional.of(extra));

        service.removeMenuItem(menuItem, 10L);

        assertTrue(extra.getMenuItems().isEmpty());
        verify(repository).findById(10L);
        verify(repository).save(extra);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void removeMenuItem_throwsWhenTheExtraDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class, () -> service.removeMenuItem(new MenuItem(), 99L));

        assertEquals("Extra 99 not found", exception.getMessage());
        verify(repository).findById(99L);
        verifyNoMoreInteractions(repository);
    }
}