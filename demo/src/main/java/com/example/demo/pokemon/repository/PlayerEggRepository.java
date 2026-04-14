package com.example.demo.pokemon.repository;

import com.example.demo.pokemon.entity.PlayerEgg;
import com.example.demo.pokemon.enums.EggStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerEggRepository extends JpaRepository<PlayerEgg, Long> {

    List<PlayerEgg> findByUserIdOrderByCreatedAtAsc(Long userId);

    List<PlayerEgg> findByUserIdAndStatusInOrderByCreatedAtAsc(Long userId, List<EggStatus> statuses);

    Optional<PlayerEgg> findByIdAndUserId(Long id, Long userId);
}
