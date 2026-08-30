package com.example.uc4monitor.repository;

import com.example.uc4monitor.config.Uc4Properties;
import com.example.uc4monitor.domain.Uc4JobDefinition;
import com.example.uc4monitor.domain.Uc4JobRunHistory;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class Uc4SourceRepository {

    private final NamedParameterJdbcTemplate uc4JdbcTemplate;
    private final Uc4Properties properties;
    private final String definitionQuery;
    private final String runHistoryQuery;

    public Uc4SourceRepository(
            @Qualifier("uc4NamedJdbcTemplate") NamedParameterJdbcTemplate uc4JdbcTemplate,
            Uc4Properties properties,
            SqlResourceLoader sqlResourceLoader
    ) {
        this.uc4JdbcTemplate = uc4JdbcTemplate;
        this.properties = properties;
        this.definitionQuery = sqlResourceLoader.read(properties.sync().definitionQueryLocation());
        this.runHistoryQuery = sqlResourceLoader.read(properties.sync().runHistoryQueryLocation());
    }

    public List<Uc4JobDefinition> findTeamDefinitions() {
        return uc4JdbcTemplate.query(
                definitionQuery,
                commonParams(LocalDate.now().minusDays(properties.lookbackDays())),
                new DefinitionMapper()
        );
    }

    public List<Uc4JobRunHistory> findRecentRunHistory() {
        return uc4JdbcTemplate.query(
                runHistoryQuery,
                commonParams(LocalDate.now().minusDays(properties.lookbackDays())),
                new RunHistoryMapper()
        );
    }

    private Map<String, Object> commonParams(LocalDate lookbackStart) {
        Map<String, Object> params = new HashMap<>();
        params.put("teamCode", properties.teamCode());
        params.put("teamNamePattern", properties.teamNamePattern());
        params.put("uc4Client", properties.client());
        params.put("lookbackStart", Date.valueOf(lookbackStart));
        return params;
    }

    private static final class DefinitionMapper implements RowMapper<Uc4JobDefinition> {
        @Override
        public Uc4JobDefinition mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Uc4JobDefinition(
                    rs.getString("uc4_object_id"),
                    rs.getString("job_name"),
                    rs.getString("object_type"),
                    rs.getString("plan_name"),
                    rs.getString("folder_path"),
                    rs.getString("team_code"),
                    rs.getBoolean("active")
            );
        }
    }

    private static final class RunHistoryMapper implements RowMapper<Uc4JobRunHistory> {
        @Override
        public Uc4JobRunHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp startTime = rs.getTimestamp("start_time");
            Timestamp endTime = rs.getTimestamp("end_time");
            Date businessDate = rs.getDate("business_date");
            return new Uc4JobRunHistory(
                    rs.getString("uc4_run_id"),
                    rs.getString("job_name"),
                    rs.getString("plan_name"),
                    startTime == null ? null : startTime.toLocalDateTime(),
                    endTime == null ? null : endTime.toLocalDateTime(),
                    getNullableLong(rs, "duration_seconds"),
                    rs.getString("status"),
                    getNullableInteger(rs, "return_code"),
                    rs.getString("last_report"),
                    businessDate == null ? null : businessDate.toLocalDate()
            );
        }

        private static Long getNullableLong(ResultSet rs, String column) throws SQLException {
            long value = rs.getLong(column);
            return rs.wasNull() ? null : value;
        }

        private static Integer getNullableInteger(ResultSet rs, String column) throws SQLException {
            int value = rs.getInt(column);
            return rs.wasNull() ? null : value;
        }
    }
}
