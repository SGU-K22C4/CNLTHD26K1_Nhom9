package com.fashion.productservice.repository;

import com.fashion.productservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    List<Category> findByParentIsNullAndVisibleTrue();
    List<Category> findByParentIdAndVisibleTrue(String parentId);
}
