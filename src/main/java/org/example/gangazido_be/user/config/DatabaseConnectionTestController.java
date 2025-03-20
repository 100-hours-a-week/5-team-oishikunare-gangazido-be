package org.example.gangazido_be.user.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class DatabaseConnectionTestController {

	private final DataSource dataSource;
	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public DatabaseConnectionTestController(DataSource dataSource, JdbcTemplate jdbcTemplate) {
		this.dataSource = dataSource;
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/db-status")
	public ResponseEntity<Map<String, Object>> checkDatabaseConnection() {
		Map<String, Object> response = new HashMap<>();

		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();

			response.put("status", "connected");
			response.put("databaseProduct", metaData.getDatabaseProductName());
			response.put("databaseVersion", metaData.getDatabaseProductVersion());
			response.put("driverName", metaData.getDriverName());
			response.put("driverVersion", metaData.getDriverVersion());
			response.put("url", metaData.getURL());

			// 실제 쿼리 실행으로 활성 연결 확인
			Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
			response.put("queryResult", result);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.put("status", "error");
			response.put("message", e.getMessage());
			response.put("exception", e.getClass().getName());
			return ResponseEntity.status(500).body(response);
		}
	}

	@GetMapping("/db-tables")
	public ResponseEntity<Map<String, Object>> listTables() {
		Map<String, Object> response = new HashMap<>();

		try {
			// 사용 가능한 테이블 목록 가져오기
			jdbcTemplate.query(
				"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?",
				new Object[] { getSchema() },
				(rs, rowNum) -> rs.getString("TABLE_NAME")
			).forEach(tableName -> {
				try {
					// 각 테이블의 행 수 확인
					Integer count = jdbcTemplate.queryForObject(
						"SELECT COUNT(*) FROM " + tableName, Integer.class);
					response.put(tableName, count);
				} catch (Exception e) {
					response.put(tableName + "_error", e.getMessage());
				}
			});

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.put("status", "error");
			response.put("message", e.getMessage());
			return ResponseEntity.status(500).body(response);
		}
	}

	private String getSchema() {
		try {
			return dataSource.getConnection().getCatalog();
		} catch (Exception e) {
			return "gangazidoDB"; // 기본값 설정
		}
	}
}
