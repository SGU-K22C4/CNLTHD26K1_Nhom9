package com.fashion.productservice.mapper;

import com.fashion.productservice.dto.response.ImageResponse;
import com.fashion.productservice.dto.response.ProductResponse;
import com.fashion.productservice.dto.response.SizeResponse;
import com.fashion.productservice.dto.response.VariantResponse;
import com.fashion.productservice.entity.Product;
import com.fashion.productservice.entity.ProductVariant;
import com.fashion.productservice.entity.VariantImage;
import com.fashion.productservice.entity.VariantSize;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryGender", source = "category.gender")
    ProductResponse toResponse(Product product);

    @Mapping(target = "images", source = "images", qualifiedByName = "toSortedImageResponses")
    VariantResponse toResponse(ProductVariant variant);

    ImageResponse toResponse(VariantImage image);

    SizeResponse toResponse(VariantSize size);

    @Named("toSortedImageResponses")
    default List<ImageResponse> toSortedImageResponses(List<VariantImage> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream()
                .sorted(Comparator.comparingInt(VariantImage::getSortOrder))
                .map(this::toResponse)
                .toList();
    }

    default String map(Enum<?> value) {
        return value == null ? null : value.name();
    }
}