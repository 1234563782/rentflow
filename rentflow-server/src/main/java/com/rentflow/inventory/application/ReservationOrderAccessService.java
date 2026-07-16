package com.rentflow.inventory.application;

import com.rentflow.inventory.api.LockedReservationForOrder;
import com.rentflow.inventory.api.ReservationOrderAccess;
import com.rentflow.inventory.infrastructure.ReservationMapper;
import com.rentflow.shared.id.Ulid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ReservationOrderAccessService implements ReservationOrderAccess {
    private final ReservationMapper reservationMapper;

    public ReservationOrderAccessService(ReservationMapper reservationMapper) {
        this.reservationMapper = reservationMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<LockedReservationForOrder> lockReservation(String reservationId) {
        return reservationMapper.lockForOrder(Ulid.requireValid(reservationId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int consumeActive(String reservationId) {
        return reservationMapper.consumeActive(Ulid.requireValid(reservationId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int releaseForOrder(String reservationId) {
        return reservationMapper.releaseForOrder(Ulid.requireValid(reservationId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int expireForOrder(String reservationId) {
        return reservationMapper.expireForOrder(Ulid.requireValid(reservationId));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int assignEquipment(String reservationId, String equipmentUnitId) {
        return reservationMapper.assignEquipment(
                Ulid.requireValid(reservationId), Ulid.requireValid(equipmentUnitId)
        );
    }
}
