package ru.practicum.mainservice.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mainservice.category.dto.CategoryDto;
import ru.practicum.mainservice.category.dto.NewCategoryDto;
import ru.practicum.mainservice.category.mapper.CategoryMapper;
import ru.practicum.mainservice.category.model.Category;
import ru.practicum.mainservice.category.repository.CategoryRepository;
import ru.practicum.mainservice.exception.exception.NotFoundException;

import java.util.List;

import static ru.practicum.mainservice.util.LogColorizeUtil.colorizeClass;
import static ru.practicum.mainservice.util.LogColorizeUtil.colorizeMethod;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryDto create(NewCategoryDto newCategoryDto) {
        log.info("{}: Starting execution of {} method.", colorizeClass("CategoryService"), colorizeMethod("create()"));
        log.info("{}.{}: Mapping from NewCategoryDto to Category.", colorizeClass("CategoryService"), colorizeMethod("create()"));

        Category category = categoryMapper.fromNewCategoryDto(newCategoryDto);

        log.info("{}.{}: Saving category to database.", colorizeClass("CategoryService"), colorizeMethod("create()"));
        category = categoryRepository.save(category);

        log.info("{}.{}: Category saved successfully with id={}.", colorizeClass("CategoryService"), colorizeMethod("create()"), category.getId());
        log.info("{}.{}: Mapping from Category to CategoryDto.", colorizeClass("CategoryService"), colorizeMethod("create()"));

        CategoryDto categoryDto = categoryMapper.toCategoryDto(category);

        log.info("{}.{}: Returning CategoryDto with id={}.", colorizeClass("CategoryService"), colorizeMethod("create()"), categoryDto.getId());
        return categoryDto;
    }

    @Override
    @Transactional
    public void delete(Long catId) {
        log.info("{}: Starting execution of {} method.", colorizeClass("CategoryService"), colorizeMethod("delete()"));
        log.info("{}.{}: Checking if category exists with id={}.", colorizeClass("CategoryService"), colorizeMethod("delete()"), catId);

        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException(String.format("Category with id=%d was not found.", catId));
        }

        log.info("{}.{}: Deleting category with id={}.", colorizeClass("CategoryService"), colorizeMethod("delete()"), catId);
        categoryRepository.deleteById(catId);

        log.info("{}.{}: Category with id={} deleted successfully.", colorizeClass("CategoryService"), colorizeMethod("delete()"), catId);
    }

    @Override
    @Transactional
    public CategoryDto update(Long catId, CategoryDto categoryDto) {
        log.info("{}: Starting execution of {} method.", colorizeClass("CategoryService"), colorizeMethod("update()"));
        log.info("{}.{}: Checking if category exists with id={}.", colorizeClass("CategoryService"), colorizeMethod("update()"), catId);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException(String.format("Category with id=%d was not found.", catId)));

        log.info("{}.{}: Updating category with id={}.", colorizeClass("CategoryService"), colorizeMethod("update()"), catId);

        category.setName(categoryDto.getName());
        categoryRepository.save(category);

        log.info("{}.{}: Category with id={} updated successfully.", colorizeClass("CategoryService"), colorizeMethod("update()"), catId);

        CategoryDto categoryDtoUpdated = categoryMapper.toCategoryDto(category);

        log.info("{}.{}: Returning updated CategoryDto for category id={}.", colorizeClass("CategoryService"), colorizeMethod("update()"), catId);
        return categoryDtoUpdated;
    }

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        log.info("{}: Starting execution of {} method.", colorizeClass("CategoryService"), colorizeMethod("getCategories()"));
        log.info("{}.{}: Fetching categories with pagination from={} size={}.", colorizeClass("CategoryService"), colorizeMethod("getCategories()"), from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Category> categoryPage = categoryRepository.findAll(pageable);

        if (categoryPage.isEmpty()) {
            log.info("{}.{}: No categories found.", colorizeClass("CategoryService"), colorizeMethod("getCategories()"));
            return List.of();
        }

        List<CategoryDto> categories = categoryPage.stream()
                .map(categoryMapper::toCategoryDto)
                .toList();

        log.info("{}.{}: Returning {} categories.", colorizeClass("CategoryService"), colorizeMethod("getCategories()"), categories.size());
        return categories;
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        log.info("{}: Starting execution of {} method.", colorizeClass("CategoryService"), colorizeMethod("getCategoryById()"));
        log.info("{}.{}: Fetching category with id={}.", colorizeClass("CategoryService"), colorizeMethod("getCategoryById()"), catId);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException(String.format("Category with id=%d was not found.", catId)));

        log.info("{}.{}: Category with id={} found.", colorizeClass("CategoryService"), colorizeMethod("getCategoryById()"), catId);

        CategoryDto categoryDto = categoryMapper.toCategoryDto(category);

        log.info("{}.{}: Returning CategoryDto with id={}.", colorizeClass("CategoryService"), colorizeMethod("getCategoryById()"), categoryDto.getId());
        return categoryDto;
    }
}