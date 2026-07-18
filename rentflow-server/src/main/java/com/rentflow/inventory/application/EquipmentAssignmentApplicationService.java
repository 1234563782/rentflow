package com.rentflow.inventory.application;

import com.rentflow.inventory.api.AssignedEquipment;
import com.rentflow.inventory.api.EquipmentAssignmentAllocator;
import com.rentflow.inventory.infrastructure.EquipmentCandidate;
import com.rentflow.inventory.infrastructure.InventoryLockMapper;
import com.rentflow.shared.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class EquipmentAssignmentApplicationService implements EquipmentAssignmentAllocator {
    private final InventoryLockMapper inventoryLockMapper;

    public EquipmentAssignmentApplicationService(InventoryLockMapper inventoryLockMapper) {
        this.inventoryLockMapper = inventoryLockMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public AssignedEquipment assign(String productId, LocalDate startDate, LocalDate endDate) {
        for (EquipmentCandidate candidate : inventoryLockMapper.listAllocationCandidates(productId)) {
            Optional<EquipmentCandidate> locked = inventoryLockMapper.lockAllocationCandidate(
                    productId, candidate.equipmentUnitId()
            );
            if (locked.isEmpty()) {
                continue;
            }
            if (inventoryLockMapper.lockActiveReservationConflict(
                    candidate.equipmentUnitId(), startDate, endDate
            ).isPresent()) {
                continue;
            }
            if (inventoryLockMapper.lockConfirmedOrderConflict(
                    candidate.equipmentUnitId(), startDate, endDate
            ).isEmpty()) {
                EquipmentCandidate equipment = locked.get();
                return new AssignedEquipment(equipment.equipmentUnitId(), equipment.displayCode());
            }
        }
        throw new BusinessException(
                "EQUIPMENT_ASSIGNMENT_UNAVAILABLE",
                "No concrete equipment can be assigned for the rental period",
                HttpStatus.CONFLICT
        );
    }
}
