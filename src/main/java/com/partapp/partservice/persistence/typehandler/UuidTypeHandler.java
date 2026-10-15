package com.partapp.partservice.persistence.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** Maps Java UUID values to PostgreSQL's native uuid type. */
public class UuidTypeHandler extends BaseTypeHandler<UUID> {
  @Override
  public void setNonNullParameter(PreparedStatement statement, int index, UUID value, JdbcType jdbcType)
      throws SQLException {
    statement.setObject(index, value, Types.OTHER);
  }

  @Override
  public UUID getNullableResult(ResultSet result, String column) throws SQLException {
    return result.getObject(column, UUID.class);
  }

  @Override
  public UUID getNullableResult(ResultSet result, int column) throws SQLException {
    return result.getObject(column, UUID.class);
  }

  @Override
  public UUID getNullableResult(CallableStatement statement, int column) throws SQLException {
    return statement.getObject(column, UUID.class);
  }
}
