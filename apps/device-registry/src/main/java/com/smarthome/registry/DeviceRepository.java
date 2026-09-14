package com.smarthome.registry;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class DeviceRepository {

    private static final String DELETED_BY_SYNC = "monolith-sync";

    private static final RowMapper<Device> MAPPER = (ResultSet rs, int rowNum) -> new Device(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("type"),
            rs.getString("location"),
            rs.getString("unit"),
            rs.getString("status"),
            offsetDateTime(rs, "registered_at"),
            offsetDateTime(rs, "updated_at"),
            offsetDateTime(rs, "deleted_at"),
            rs.getString("deleted_by"));

    private final NamedParameterJdbcTemplate jdbc;

    public DeviceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Device> findAll(boolean includeDeleted) {
        String sql = "SELECT * FROM devices"
                + (includeDeleted ? "" : " WHERE deleted_at IS NULL")
                + " ORDER BY id";
        return jdbc.query(sql, Map.of(), MAPPER);
    }

    public Optional<Device> findById(int id) {
        return jdbc.query("SELECT * FROM devices WHERE id = :id", Map.of("id", id), MAPPER)
                .stream()
                .findFirst();
    }

    public void upsert(MonolithSensor sensor) {
        String sql = """
                INSERT INTO devices (id, status, unit, type, name, location)
                VALUES (:id, :status, :unit, :type, :name, :location)
                ON CONFLICT (id) DO UPDATE SET
                    status     = EXCLUDED.status,
                    unit       = EXCLUDED.unit,
                    type       = EXCLUDED.type,
                    name       = EXCLUDED.name,
                    location   = EXCLUDED.location,
                    updated_at = now(),
                    deleted_at = NULL,
                    deleted_by = NULL
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", sensor.id())
                .addValue("status", sensor.status())
                .addValue("unit", sensor.unit())
                .addValue("type", sensor.type())
                .addValue("name", sensor.name())
                .addValue("location", sensor.location()));
    }

    public int markMissingDeleted(Collection<Integer> aliveIds) {
        if (aliveIds.isEmpty()) {
            return jdbc.update(
                    "UPDATE devices SET deleted_at = now(), deleted_by = :by WHERE deleted_at IS NULL",
                    Map.of("by", DELETED_BY_SYNC));
        }
        return jdbc.update(
                "UPDATE devices SET deleted_at = now(), deleted_by = :by "
                        + "WHERE deleted_at IS NULL AND id NOT IN (:ids)",
                new MapSqlParameterSource().addValue("by", DELETED_BY_SYNC).addValue("ids", aliveIds));
    }

    private static OffsetDateTime offsetDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class);
    }
}
