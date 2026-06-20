package org.philipp.fun.minidev.page.repository;

import java.util.List;

import org.philipp.fun.minidev.page.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Page entities.
 */
@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    /**
     * Finds all enabled pages ordered by navigation order.
     *
     * @return list of enabled pages
     */
    List<Page> findByEnabledTrueOrderByNavOrderAsc();

    /**
     * Finds enabled pages accessible by a given role.
     *
     * @param role the role to filter by
     * @return list of matching pages
     */
    List<Page> findByEnabledTrueAndRoleRequiredIsNullOrRoleRequiredOrderByNavOrderAsc(String role);
}