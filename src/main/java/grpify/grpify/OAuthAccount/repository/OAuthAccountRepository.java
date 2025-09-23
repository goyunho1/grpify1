package grpify.grpify.OAuthAccount.repository;

import grpify.grpify.OAuthAccount.domain.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {
    Optional<OAuthAccount> findByProviderAndProviderId(String provider, String providerId);

    Optional<OAuthAccount> findByUserId(Long userId);
}
