package com.mxverse.storage.r2vault.repository;

import com.mxverse.storage.r2vault.model.AccountKey;
import com.mxverse.storage.r2vault.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AccountKeyRepository extends JpaRepository<AccountKey, Long> {
    Optional<AccountKey> findByUser(User user);

    Optional<AccountKey> findByUserId(Long userId);
}
