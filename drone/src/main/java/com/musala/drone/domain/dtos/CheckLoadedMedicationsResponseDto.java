package com.musala.drone.domain.dtos;

import com.musala.drone.model.ApiResponseDto;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CheckLoadedMedicationsResponseDto extends ApiResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private List<LoadedMedications> data;

}
