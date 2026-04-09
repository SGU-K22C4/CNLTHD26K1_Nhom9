package com.fashion.reviewservice.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {

    @NotBlank(message = "orderId không được để trống")
    private String orderId;

    @NotBlank(message = "productId không được để trống")
    private String productId;

    @NotNull(message = "Số sao không được để trống")
    @Min(value = 1, message = "Số sao tối thiểu là 1")
    @Max(value = 5, message = "Số sao tối đa là 5")
    @JsonAlias("rating")
    private Integer star;

    @Size(max = 120, message = "Tiêu đề tối đa 120 ký tự")
    private String title;

    @NotBlank(message = "Nội dung đánh giá không được để trống")
    @Size(max = 1200, message = "Nội dung đánh giá tối đa 1200 ký tự")
    @JsonAlias("comment")
    private String content;

    @JsonAlias("imageUrls")
    private List<String> images;
}
