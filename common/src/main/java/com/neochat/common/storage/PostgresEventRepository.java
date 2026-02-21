package com.neochat.common.storage;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class PostgresEventRepository
        implements PanacheRepositoryBase<PostgresEventStore.PostgresEventEntity, UUID> {
}
