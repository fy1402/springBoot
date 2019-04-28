package example.chapter01.server;

import java.sql.*;

public class RecordQuery {

    private final Connection connection;

    public RecordQuery(Connection connection) {
        this.connection = connection;
    }

    public <T> T query(RowHandler<T> handler, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)){
            int index = 1;
            for (Object param : params) {
                stmt.setObject(index++, param);
            }
            ResultSet resultSet = stmt.executeQuery();
            return handler.handle(resultSet); // 1 调用 RowHandler
        }
    }
}
