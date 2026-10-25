package be.vives.pizzastore.mapper;

import be.vives.pizzastore.domain.NutritionalInfo;
import be.vives.pizzastore.dto.request.NutritionalInfoRequest;
import be.vives.pizzastore.dto.response.NutritionalInfoResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NutritionalInfoMapper {

    NutritionalInfoResponse toResponse(NutritionalInfo nutritionalInfo);

    NutritionalInfo toEntity(NutritionalInfoRequest request);
}
