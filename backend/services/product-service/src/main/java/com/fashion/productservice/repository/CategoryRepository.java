package com.fashion.productservice.repository;

import com.fashion.productservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// XÓA 2 DÒNG @Mock và khai báo biến ở đây đi nhé!

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    List<Category> findByParentIsNullAndVisibleTrue();
    List<Category> findByParentIdAndVisibleTrue(String parentId);
}