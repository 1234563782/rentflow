package com.rentflow.inventory.application;

import com.rentflow.inventory.api.AssignedEquipment;
import com.rentflow.inventory.infrastructure.EquipmentCandidate;
import com.rentflow.inventory.infrastructure.InventoryLockMapper;
import com.rentflow.shared.web.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EquipmentAssignmentApplicationServiceTest {
    private static final String PRODUCT_ID = "01J00000000000000000000101";
    private static final String FIRST_EQUIPMENT_ID = "01J00000000000000000001001";
    private static final String SECOND_EQUIPMENT_ID = "01J00000000000000000001002";
    private static final LocalDate START = LocalDate.parse("2026-07-20");
    private static final LocalDate END = LocalDate.parse("2026-07-21");

    @Test
    void skipsConflictingUnitAndReturnsNextLockableUnit() {
        InventoryLockMapper inventory = mock(InventoryLockMapper.class);
        EquipmentCandidate first = new EquipmentCandidate(FIRST_EQUIPMENT_ID, "RF-0001");
        EquipmentCandidate second = new EquipmentCandidate(SECOND_EQUIPMENT_ID, "RF-0002");
        when(inventory.listAllocationCandidates(PRODUCT_ID)).thenReturn(List.of(first, second));
        when(inventory.lockAllocationCandidate(PRODUCT_ID, FIRST_EQUIPMENT_ID))
                .thenReturn(Optional.of(first));
        when(inventory.lockAllocationCandidate(PRODUCT_ID, SECOND_EQUIPMENT_ID))
                .thenReturn(Optional.of(second));
        when(inventory.lockConfirmedOrderConflict(FIRST_EQUIPMENT_ID, START, END))
                .thenReturn(Optional.of("01J00000000000000000040001"));
        when(inventory.lockConfirmedOrderConflict(SECOND_EQUIPMENT_ID, START, END))
                .thenReturn(Optional.empty());

        AssignedEquipment result = new EquipmentAssignmentApplicationService(inventory)
                .assign(PRODUCT_ID, START, END);

        assertThat(result).isEqualTo(new AssignedEquipment(SECOND_EQUIPMENT_ID, "RF-0002"));
    }

    @Test
    void rejectsAssignmentWhenEveryConcreteUnitConflicts() {
        InventoryLockMapper inventory = mock(InventoryLockMapper.class);
        EquipmentCandidate equipment = new EquipmentCandidate(FIRST_EQUIPMENT_ID, "RF-0001");
        when(inventory.listAllocationCandidates(PRODUCT_ID)).thenReturn(List.of(equipment));
        when(inventory.lockAllocationCandidate(PRODUCT_ID, FIRST_EQUIPMENT_ID))
                .thenReturn(Optional.of(equipment));
        when(inventory.lockActiveReservationConflict(FIRST_EQUIPMENT_ID, START, END))
                .thenReturn(Optional.of("01J00000000000000000020001"));

        assertThatThrownBy(() -> new EquipmentAssignmentApplicationService(inventory)
                .assign(PRODUCT_ID, START, END))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.code()).isEqualTo("EQUIPMENT_ASSIGNMENT_UNAVAILABLE")
                );
    }
}
