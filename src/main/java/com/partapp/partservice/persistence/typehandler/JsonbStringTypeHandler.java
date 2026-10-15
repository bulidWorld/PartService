package com.partapp.partservice.persistence.typehandler;

import java.sql.*;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

public class JsonbStringTypeHandler extends BaseTypeHandler<String> {
  @Override public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
    PGobject json = new PGobject(); json.setType("jsonb"); json.setValue(parameter); ps.setObject(i, json);
  }
  @Override public String getNullableResult(ResultSet rs, String columnName) throws SQLException { return rs.getString(columnName); }
  @Override public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException { return rs.getString(columnIndex); }
  @Override public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException { return cs.getString(columnIndex); }
}
