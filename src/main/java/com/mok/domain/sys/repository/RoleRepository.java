package com.mok.domain.sys.repository;


import com.mok.domain.sys.model.Role;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    @Query("select count(u) > 0 from com.mok.ddd.domain.sys.model.User u join u.roles r where r.id = :roleId")
    boolean existsUserAssociatedWithRole(Long roleId);
}