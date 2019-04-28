package example.chapter01.server;

import java.sql.ResultSet;

public interface RowHandler<T> {


    T handle(ResultSet resultSet);

}
