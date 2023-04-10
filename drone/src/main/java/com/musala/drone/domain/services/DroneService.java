package com.musala.drone.domain.services;

import com.musala.drone.domain.dtos.*;
import com.musala.drone.domain.entities.*;
import com.musala.drone.domain.enums.DroneState;
import com.musala.drone.domain.exception.CustomException;
import com.musala.drone.domain.mapper.DroneMapper;
import com.musala.drone.domain.repository.*;
import com.musala.drone.domain.util.ResourceBundleConfig;
import com.musala.drone.model.ApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Primary
@Slf4j
@Transactional
public class DroneService implements DroneIService {

    @Autowired
    private DroneRepo droneRepo;
    @Autowired
    private MedicationRepo medicationRepo;
    @Autowired
    private MedicationPhotoRepo medicationPhotoRepo;
    @Autowired
    private MedicationHistoryRepo medicationHistoryRepo;
    @Autowired
    private DroneBatteryHistoryRepo droneBatteryHistoryRepo;

    @Autowired
    ResourceBundleConfig resourceBundleConfig;

    @Autowired
    DroneMapper droneMapper;

    /**
     * These method used to register drone and log it into DB
     *
     * @param registerDroneRequestDto
     * @return
     * @throws CustomException
     */

    @Override
    public RegisterDroneResponseDto registerDrone(RegisterDroneRequestDto registerDroneRequestDto) throws CustomException {
        Drone drone = null;

        RegisterDroneResponseDto registerDroneResponseDto = new RegisterDroneResponseDto();
        try {
            drone = droneMapper.toDroneEntity(registerDroneRequestDto);
        } catch (Exception exception) {
            exception.printStackTrace();
            log.info("Failed to Map RegisterDroneRequestDto to Drone Entity");

            ApiResponseDto apiResponseDto = new ApiResponseDto();

            apiResponseDto.setCode(resourceBundleConfig.getDroneRegistrationFailedCode());
            apiResponseDto.setMessage(resourceBundleConfig.getDroneRegistrationFailedMessage());

            throw new CustomException(apiResponseDto);

        }
        droneRepo.save(drone);
        log.info("Drone is inserted");

        registerDroneResponseDto.setCode(resourceBundleConfig.getOperationDroneSuccessfullyCode());
        registerDroneResponseDto.setMessage(resourceBundleConfig.getDroneSuccessfulRegistrationMessage());

        return registerDroneResponseDto;
    }

    /**
     * These method used to load medications on drone
     *
     * @param loadDroneMedicationsRequestDto
     * @return
     * @throws CustomException
     */

    @Override
    public LoadDroneMedicationsResponseDto loadDrone(LoadDroneMedicationsRequestDto loadDroneMedicationsRequestDto) throws CustomException {

        String droneSerialNumber = loadDroneMedicationsRequestDto.getDroneSerialNumber();
        Medication medication = null;
        MedicationPhoto medicationPhoto = null;
        DroneMedicationHistory droneMedicationHistory = new DroneMedicationHistory();
        LoadDroneMedicationsResponseDto loadDroneMedicationsResponseDto = new LoadDroneMedicationsResponseDto();

        Optional<Drone> drone = droneRepo.findById(droneSerialNumber);
        if (drone.isPresent()) {
            try {
                medication = droneMapper.toMedicationEntity(loadDroneMedicationsRequestDto);
            } catch (Exception exception) {
                exception.printStackTrace();
                log.info("Failed to Map RegisterDroneRequestDto to Drone Entity");

                ApiResponseDto apiResponseDto = new ApiResponseDto();

                apiResponseDto.setCode(resourceBundleConfig.getDroneRegistrationFailedCode());
                apiResponseDto.setMessage(resourceBundleConfig.getDroneRegistrationFailedMessage());

                throw new CustomException(apiResponseDto);
            }
            medication = medicationRepo.save(medication);

            log.info("Medication has been inserted successfully");
            try {
                medicationPhoto = droneMapper.toMedicationPhotoEntity(loadDroneMedicationsRequestDto);
            } catch (Exception exception) {

            }
            if (loadDroneMedicationsRequestDto.getImage() != null) {
                medicationPhotoRepo.save(medicationPhoto);
                log.info("MedicationPhoto has been inserted successfully");
            }

            droneMedicationHistory.setDroneId(droneSerialNumber);
            droneMedicationHistory.setMedicationId(medication.getCode());
            droneMedicationHistory.setLoadingTime(new Timestamp(System.currentTimeMillis()));
            medicationHistoryRepo.save(droneMedicationHistory);
            log.info("DroneMedicationHistory has been inserted successfully");

            loadDroneMedicationsResponseDto.setCode("600");
            loadDroneMedicationsResponseDto.setMessage("Medications loaded successfully to the drone");
        } else {

            log.info("There is no drone with this serialNumber to load Medications on it");

            ApiResponseDto apiResponseDto = new ApiResponseDto();

            apiResponseDto.setCode(resourceBundleConfig.getDroneDoesNotPresentCode());
            apiResponseDto.setMessage(resourceBundleConfig.getDroneDoesNotPresentMessage());

            throw new CustomException(apiResponseDto);
        }
        return loadDroneMedicationsResponseDto;
    }

    /**
     * These method used to get all loaded medications on specific drone
     *
     * @param checkLoadedMedicationsRequestDto
     * @return
     * @throws CustomException
     */
    @Override
    public CheckLoadedMedicationsResponseDto checkDroneLoadedMedications(CheckLoadedMedicationsRequestDto checkLoadedMedicationsRequestDto) throws CustomException {
        CheckLoadedMedicationsResponseDto checkLoadedMedicationsResponseDto = new CheckLoadedMedicationsResponseDto();
        List<LoadedMedications> loadedMedicationsList = new ArrayList<>();
        List<DroneMedicationHistory> droneMedicationHistoryList = medicationHistoryRepo.findAllByDroneId(checkLoadedMedicationsRequestDto.getDroneSerialNumber());
        if (droneMedicationHistoryList.size() > 0) {

            for (DroneMedicationHistory dh : droneMedicationHistoryList) {
                LoadedMedications loadedMedications = new LoadedMedications();
                loadedMedications.setMedicationCode(dh.getMedicationId());
                loadedMedications.setDroneSerialNumber(dh.getDroneId());
                loadedMedicationsList.add(loadedMedications);
            }


            checkLoadedMedicationsResponseDto.setCode("600");
            checkLoadedMedicationsResponseDto.setMessage("Loaded Medications returned successfully");
            checkLoadedMedicationsResponseDto.setData(loadedMedicationsList);

        } else {

            log.info("There are no loaded medications on the drone");

            ApiResponseDto apiResponseDto = new ApiResponseDto();

            apiResponseDto.setCode(resourceBundleConfig.getLoadMedicationsFailureCode());
            apiResponseDto.setMessage(resourceBundleConfig.getLoadMedicationsFailureMessage());

            throw new CustomException(apiResponseDto);

        }

        return checkLoadedMedicationsResponseDto;
    }

    /**
     * These method used to get all available drones for loading
     *
     * @return
     * @throws CustomException
     */
    @Override
    public CheckAvailableDronesResponseDto checkAvailableDrones() throws CustomException {

        CheckAvailableDronesResponseDto checkAvailableDronesResponseDto = new CheckAvailableDronesResponseDto();
        List<DroneDto> droneDtoList = new ArrayList<>();

        List<Drone> droneList = droneRepo.getAllAvailableDronesForLoading();

        if (droneList.size() > 0) {

            for (Drone d : droneList) {
                DroneDto droneDto = new DroneDto();
                droneDto.setDroneModel(d.getDroneModel());
                droneDto.setState(d.getState().toString());
                droneDto.setWeightLimit(d.getWeightLimit());
                droneDto.setSerialNumber(d.getSerialNumber());
                droneDto.setBatteryCapacity(d.getBatteryCapacity());
                droneDtoList.add(droneDto);
            }

            checkAvailableDronesResponseDto.setCode("600");
            checkAvailableDronesResponseDto.setMessage("List of available drones are returned successfully");
            checkAvailableDronesResponseDto.setDroneDtoList(droneDtoList);

        } else {

            log.info("There are no available drones to load");

            ApiResponseDto apiResponseDto = new ApiResponseDto();

            apiResponseDto.setCode(resourceBundleConfig.getNoAvailableDronesFailureCode());
            apiResponseDto.setMessage(resourceBundleConfig.getNoAvailableDronesFailureMessage());

            throw new CustomException(apiResponseDto);
        }

        return checkAvailableDronesResponseDto;
    }

    /**
     * cron checks the drone battery life it fires each 5 minutes
     */
    @Override
    @Scheduled(cron = "0 */5 * ? * *")
    public void checkDroneBatteryPeriodic() {
        List<Drone> drones = droneRepo.findAll();
        if (drones.size() > 0) {
            for (Drone d : drones) {

                DroneBatteryHistory droneBatteryHistory = new DroneBatteryHistory();
                droneBatteryHistory.setDroneId(d.getSerialNumber());
                droneBatteryHistory.setBatteryCapacity(d.getBatteryCapacity());
                droneBatteryHistory.setCheckingTime(new Timestamp(System.currentTimeMillis()));
                droneBatteryHistoryRepo.save(droneBatteryHistory);
            }
        }
        log.info("Cron Job Is Running>>>>>>>>>>>>");

    }

    /**
     * These method used to check drone battery
     *
     * @param checkDroneBatteryRequestDto
     * @return
     * @throws CustomException
     */
    public CheckDroneBatteryResponseDto checkDroneBattery(CheckDroneBatteryRequestDto checkDroneBatteryRequestDto) throws CustomException {

        Optional<Drone> drone = droneRepo.findById(checkDroneBatteryRequestDto.getDroneSerialNumber());

        CheckDroneBatteryResponseDto checkDroneBatteryResponseDto = new CheckDroneBatteryResponseDto();

        checkDroneBatteryResponseDto.setDroneSerialNumber(checkDroneBatteryRequestDto.getDroneSerialNumber());

        if (drone.isPresent()) {

            checkDroneBatteryResponseDto.setBatteryCapacity(drone.get().getBatteryCapacity());
            checkDroneBatteryResponseDto.setCode(resourceBundleConfig.getOperationDroneSuccessfullyCode());
            checkDroneBatteryResponseDto.setMessage(resourceBundleConfig.getDroneCapacityRetrievedSuccessfullyMessage());

        } else {

            ApiResponseDto apiResponseDto = new ApiResponseDto();

            apiResponseDto.setCode(resourceBundleConfig.getDroneDoesNotPresentCode());
            apiResponseDto.setMessage(resourceBundleConfig.getDroneDoesNotPresentMessage());

            throw new CustomException(apiResponseDto);

        }

        return checkDroneBatteryResponseDto;

    }

}
