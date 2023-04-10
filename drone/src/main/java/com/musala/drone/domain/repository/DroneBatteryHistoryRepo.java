package com.musala.drone.domain.repository;

import com.musala.drone.domain.entities.DroneBatteryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DroneBatteryHistoryRepo extends JpaRepository<DroneBatteryHistory,Integer> {
}
